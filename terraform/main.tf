provider "aws" {
  region = "eu-central-1"  # Replace with your desired region
}

terraform {
  backend "s3" {
    bucket         = "dietswizard-tfstate-bucket"
    key            = "states"
    region         = "eu-central-1"
    encrypt        = true
  }
}

resource "aws_vpc" "vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = {
    Name = "myapp-vpc-eu-central-1"
  }
}

# Subnets
# - Public
resource "aws_subnet" "subnet_public_eucentral1a" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "eu-central-1a"
  cidr_block              = "10.0.0.0/21"
  map_public_ip_on_launch = true
  tags = {
    Name = "myapp-subnet-public-eucentral1a"
  }
}

resource "aws_subnet" "subnet_public_eucentral1b" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "eu-central-1b"
  cidr_block              = "10.0.16.0/21"
  map_public_ip_on_launch = true
  tags = {
    Name = "myapp-subnet-public-eucentral1b"
  }
}

# - Private
resource "aws_subnet" "subnet_private_eucentral1a" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "eu-central-1a"
  cidr_block              = "10.0.8.0/21"
  map_public_ip_on_launch = false
  tags = {
    Name = "myapp-subnet-private-eucentral1a"
  }
}

resource "aws_subnet" "subnet_private_eucentral1b" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "eu-central-1b"
  cidr_block              = "10.0.32.0/21"
  map_public_ip_on_launch = false
  tags = {
    Name = "myapp-subnet-private-eucentral1b"
  }
}


resource "aws_db_subnet_group" "db" {
  name = "myapp-db-subnet-group"

  subnet_ids = [
    aws_subnet.subnet_public_eucentral1a.id,
    aws_subnet.subnet_public_eucentral1b.id,
    aws_subnet.subnet_private_eucentral1a.id,
    aws_subnet.subnet_private_eucentral1b.id
  ]

  tags = {
    Name = "myapp-db-subnet-group"
  }
}



data "aws_iam_policy_document" "AWSLambdaTrustPolicy" {
  version = "2012-10-17"
  statement {
    actions = ["sts:AssumeRole"]
    effect  = "Allow"
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "iam_role" {
  assume_role_policy = data.aws_iam_policy_document.AWSLambdaTrustPolicy.json
  name               = "application-test-iam-role-lambda-trigger"
}

resource "aws_iam_role_policy_attachment" "iam_role_policy_attachment_lambda_vpc_access_execution" {
  role       = aws_iam_role.iam_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}


data "aws_security_group" "default" {
  vpc_id = aws_vpc.vpc.id
  name   = "default"
}

resource "aws_lambda_function" "example_lambda_test" {
  function_name = "authorizer"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.image_uri
  memory_size   = 1024
  timeout       = 10

  vpc_config {
    subnet_ids         = [aws_subnet.subnet_private_eucentral1a.id]
    security_group_ids = [data.aws_security_group.default.id]
  }
}


// POSTGRES
resource "aws_security_group" "rds-sgroup" {
  name = "rds-sgroup"

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    description = "PostgreSQL"
    cidr_blocks = ["0.0.0.0/0"] // >
  }

  ingress {
    from_port        = 5432
    to_port          = 5432
    protocol         = "tcp"
    description      = "PostgreSQL"
    ipv6_cidr_blocks = ["::/0"] // >
  }
}

resource "aws_db_instance" "postgres_instance" {
  allocated_storage      = 20
  storage_type           = "gp2"
  engine                 = "postgres"
  engine_version         = "15.3"
  instance_class         = "db.t3.micro"
  identifier             = "example-db-test"
  username               = var.postgre_id
  password               = var.postgre_pw
  publicly_accessible    = true
  parameter_group_name   = "default.postgres15"
  vpc_security_group_ids = [aws_security_group.rds-sgroup.id]
  skip_final_snapshot    = true
}

resource "aws_api_gateway_rest_api" "my_api" {
  name        = "RestAPI"
  description = "DietswizardAPI"
}

resource "aws_api_gateway_resource" "my_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_rest_api.my_api.root_resource_id
  path_part   = "api"
}

resource "aws_api_gateway_integration" "my_integration" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  resource_id = aws_api_gateway_resource.my_resource.id
  http_method = aws_api_gateway_method.my_method.http_method

  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.example_lambda_test.invoke_arn
}

resource "aws_api_gateway_authorizer" "my_authorizer" {
  name                   = "authorizer"
  rest_api_id            = aws_api_gateway_rest_api.my_api.id
  authorizer_uri         = aws_lambda_function.example_lambda_test.invoke_arn
  authorizer_credentials = aws_iam_role.iam_role.arn
  type                   = "TOKEN"
  identity_source        = "method.request.header.Authorization"
}

resource "aws_api_gateway_method" "my_method" {
  rest_api_id   = aws_api_gateway_rest_api.my_api.id
  resource_id   = aws_api_gateway_resource.my_resource.id
  http_method   = "GET"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.my_authorizer.id
}

resource "aws_api_gateway_deployment" "my_deployment" {
  depends_on = [aws_api_gateway_integration.my_integration]

  rest_api_id = aws_api_gateway_rest_api.my_api.id
  stage_name  = "v1"
  description = "Deploying my API"
}