provider "aws" {
  region = "eu-central-1" # Replace with your desired region
}

terraform {
  backend "s3" {
    bucket  = "dietswizard-tfstate-bucket"
    key     = "states"
    region  = "eu-central-1"
    encrypt = true
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
    aws_subnet.subnet_private_eucentral1a.id,
    aws_subnet.subnet_private_eucentral1b.id
  ]

  tags = {
    Name = "myapp-db-subnet-group"
  }
}

resource "aws_internet_gateway" "my_gateway" {
  vpc_id = aws_vpc.vpc.id
}
resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.my_gateway.id
  }

  tags = {
    Name = "myapp-public-route-table"
  }
}

resource "aws_route_table_association" "public_subnet_a" {
  subnet_id      = aws_subnet.subnet_public_eucentral1a.id
  route_table_id = aws_route_table.public_route_table.id
}

resource "aws_route_table_association" "public_subnet_b" {
  subnet_id      = aws_subnet.subnet_public_eucentral1b.id
  route_table_id = aws_route_table.public_route_table.id
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

data "aws_iam_policy_document" "lambda_secrets_manager_policy" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["arn:aws:secretsmanager:eu-central-1:021114833428:secret:dietswizard-db-prod"]
    effect    = "Allow"
  }
}

resource "aws_iam_role" "iam_role" {
  assume_role_policy = data.aws_iam_policy_document.AWSLambdaTrustPolicy.json
  name               = "application-test-iam-role-lambda-trigger"
}
resource "aws_iam_policy" "lambda_secrets_manager_policy" {
  name        = "lambda_secrets_manager_policy"
  path        = "/"
  description = "Allows Lambda functions to call GetSecretValue on the specified secret."
  policy      = data.aws_iam_policy_document.lambda_secrets_manager_policy.json
}

resource "aws_iam_role_policy_attachment" "lambda_secrets_manager_policy_attachment" {
  role       = aws_iam_role.iam_role.name
  policy_arn = aws_iam_policy.lambda_secrets_manager_policy.arn
}


resource "aws_iam_role_policy_attachment" "iam_role_policy_attachment_lambda_vpc_access_execution" {
  role       = aws_iam_role.iam_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

data "aws_security_group" "default" {
  vpc_id = aws_vpc.vpc.id
  name   = "default"
}

resource "aws_lambda_function" "login_lambda" {
  function_name = var.branch_name == "main" ? "login-prod" : "login-dev"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.login_image_uri
  memory_size   = 2048
  timeout       = 20

  vpc_config {
    subnet_ids = [
      aws_subnet.subnet_private_eucentral1a.id,
      aws_subnet.subnet_private_eucentral1b.id
    ]
    security_group_ids = [data.aws_security_group.default.id]
  }
}

resource "aws_lambda_function" "register_lambda" {
  function_name = var.branch_name == "main" ? "register-prod" : "register-dev"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.register_image_uri
  memory_size   = 2048
  timeout       = 20

  vpc_config {
    subnet_ids = [
      aws_subnet.subnet_private_eucentral1a.id,
      aws_subnet.subnet_private_eucentral1b.id
    ]
    security_group_ids = [data.aws_security_group.default.id]
  }
}


# RDS Security Group
resource "aws_security_group" "rds-sgroup" {
  vpc_id = aws_vpc.vpc.id # specify the VPC
  name   = "rds-sgroup"

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    description = "PostgreSQL from Lambda"
    cidr_blocks = ["0.0.0.0/0"]
    security_groups = [data.aws_security_group.default.id] # reference the Lambda's security group
  }
}

# RDS Instance
resource "aws_db_instance" "postgres_instance" {
  allocated_storage      = 20
  storage_type           = "gp2"
  engine                 = "postgres"
  engine_version         = "15.3"
  instance_class         = "db.t3.micro"
  identifier             = var.branch_name == "main" ? "dietswizard-db-prod" : "dietswizard-db-dev"
  username               = var.postgre_id
  password               = var.postgre_pw
  parameter_group_name   = "default.postgres15"
  vpc_security_group_ids = [aws_security_group.rds-sgroup.id]
  db_subnet_group_name   = aws_db_subnet_group.db.name # reference the DB subnet group
  skip_final_snapshot    = true
  depends_on             = [aws_internet_gateway.my_gateway]
  publicly_accessible    = true
}

# Rest of the API Gateway resources remain unchanged


resource "aws_api_gateway_rest_api" "my_api" {
  name        = var.branch_name == "main" ? "dietswizard-restAPI-prod" : "dietswizard-restAPI-dev"
  description = "DietswizardAPI"
}

resource "aws_api_gateway_resource" "my_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_rest_api.my_api.root_resource_id
  path_part   = "auth"
}

resource "aws_api_gateway_resource" "register_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_resource.my_resource.id
  path_part   = "register"
}

resource "aws_api_gateway_method" "register_method" {
  rest_api_id   = aws_api_gateway_rest_api.my_api.id
  resource_id   = aws_api_gateway_resource.register_resource.id
  http_method   = "POST"
  authorization = "None"
}


resource "aws_api_gateway_resource" "login_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_resource.my_resource.id
  path_part   = "login"
}

resource "aws_api_gateway_method" "login_method" {
  rest_api_id   = aws_api_gateway_rest_api.my_api.id
  resource_id   = aws_api_gateway_resource.login_resource.id
  http_method   = "POST"
  authorization = "None"
}

resource "aws_api_gateway_deployment" "my_deployment" {
  depends_on = [
    aws_api_gateway_integration.register_integration,
    aws_api_gateway_method.register_method,
    aws_api_gateway_integration.login_integration,
    aws_api_gateway_method.login_method,
    aws_api_gateway_integration.me_integration,
    aws_api_gateway_method.me_method,
    aws_api_gateway_integration.refresh_integration,
    aws_api_gateway_method.refresh_method
  ]

  rest_api_id = aws_api_gateway_rest_api.my_api.id
  stage_name  = var.branch_name == "main" ? "prod" : "dev"
  description = "Deploying my API"
}


resource "aws_api_gateway_integration" "register_integration" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  resource_id = aws_api_gateway_resource.register_resource.id
  http_method = aws_api_gateway_method.register_method.http_method

  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.register_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "login_integration" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  resource_id = aws_api_gateway_resource.login_resource.id
  http_method = aws_api_gateway_method.login_method.http_method

  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.login_lambda.invoke_arn
}

data "aws_iam_policy_document" "api_gateway_invoke" {
  statement {
    actions   = ["execute-api:Invoke"]
    resources = ["arn:aws:execute-api:eu-central-1:021114833428:*"]
    effect    = "Allow"
  }
}

resource "aws_iam_policy" "api_gateway_invoke_policy" {
  name        = "APIGatewayInvokePolicy"
  path        = "/"
  description = "Allows the Lambda function to be invoked by the API Gateway"
  policy      = data.aws_iam_policy_document.api_gateway_invoke.json
}

resource "aws_iam_role_policy_attachment" "api_gateway_invoke_policy_attachment" {
  role       = aws_iam_role.iam_role.name
  policy_arn = aws_iam_policy.api_gateway_invoke_policy.arn
}

resource "aws_lambda_permission" "apigw_register_lambda_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.register_lambda.function_name
  principal     = "apigateway.amazonaws.com"
}

resource "aws_lambda_permission" "apigw_login_lambda_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.login_lambda.function_name
  principal     = "apigateway.amazonaws.com"
}


resource "aws_api_gateway_resource" "me_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_resource.my_resource.id
  path_part   = "me"
}

resource "aws_api_gateway_method" "me_method" {
  rest_api_id   = aws_api_gateway_rest_api.my_api.id
  resource_id   = aws_api_gateway_resource.me_resource.id
  http_method   = "POST"
  authorization = "None"
}

resource "aws_lambda_permission" "apigw_me_lambda_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.me_lambda.function_name
  principal     = "apigateway.amazonaws.com"
}

resource "aws_api_gateway_integration" "me_integration" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  resource_id = aws_api_gateway_resource.me_resource.id
  http_method = aws_api_gateway_method.me_method.http_method

  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.me_lambda.invoke_arn
}

resource "aws_lambda_function" "me_lambda" {
  function_name = var.branch_name == "main" ? "me-prod" : "me-dev"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.me_image_uri
  memory_size   = 2048
  timeout       = 20

  vpc_config {
    subnet_ids = [
      aws_subnet.subnet_private_eucentral1a.id,
      aws_subnet.subnet_private_eucentral1b.id
    ]
    security_group_ids = [data.aws_security_group.default.id]
  }
}

resource "aws_api_gateway_resource" "refresh_resource" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  parent_id   = aws_api_gateway_resource.my_resource.id
  path_part   = "refresh"
}

resource "aws_api_gateway_method" "refresh_method" {
  rest_api_id   = aws_api_gateway_rest_api.my_api.id
  resource_id   = aws_api_gateway_resource.refresh_resource.id
  http_method   = "POST"
  authorization = "None"
}

resource "aws_lambda_permission" "apigw_refresh_lambda_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.refresh_lambda.function_name
  principal     = "apigateway.amazonaws.com"
}

resource "aws_api_gateway_integration" "refresh_integration" {
  rest_api_id = aws_api_gateway_rest_api.my_api.id
  resource_id = aws_api_gateway_resource.refresh_resource.id
  http_method = aws_api_gateway_method.refresh_method.http_method

  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.refresh_lambda.invoke_arn
}

resource "aws_lambda_function" "refresh_lambda" {
  function_name = var.branch_name == "main" ? "refresh-prod" : "refresh-dev"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.refresh_image_uri
  memory_size   = 2048
  timeout       = 20

  vpc_config {
    subnet_ids = [
      aws_subnet.subnet_private_eucentral1a.id,
      aws_subnet.subnet_private_eucentral1b.id
    ]
    security_group_ids = [data.aws_security_group.default.id]
  }
}