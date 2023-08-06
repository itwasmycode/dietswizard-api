provider "aws" {
  region = "eu-central-1"  # Replace with your desired region
}

resource "aws_vpc" "test_vpc" {
  cidr_block          = "10.0.0.0/16"
  enable_dns_hostnames = true
}

resource "aws_subnet" "test_subnet_1" {
  vpc_id            = aws_vpc.test_vpc.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "eu-central-1a"
}

resource "aws_subnet" "test_subnet_2" {
  vpc_id            = aws_vpc.test_vpc.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "eu-central-1b"
}

resource "aws_security_group" "public_sg" {
  name_prefix = "public-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "application_sg" {
  name_prefix = "app-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.3.0/24"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds_sg" {
  name_prefix = "rds-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.application_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "example" {
  name       = "postgres-subnet-group"
  subnet_ids = [aws_subnet.test_subnet_1.id, aws_subnet.test_subnet_2.id]
}

resource "aws_db_instance" "postgresql" {
  identifier             = "example-db"
  engine                 = "postgres"
  engine_version         = "15.3"
  instance_class         = "db.t3.micro"
  username               = var.postgre_id
  password               = var.postgre_pw
  allocated_storage      = 20
  publicly_accessible    = false
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.example.name
}

data "aws_caller_identity" "source" {
  provider = aws.source
}

data "aws_iam_policy_document" "assume_role" {
  provider = aws.destination
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.source.account_id}:root"]
    }
  }
}
data "aws_iam_policy" "ec2" {
  provider = aws.destination
  name     = "AmazonEC2FullAccess"
}

resource "aws_iam_role" "assume_role" {
  provider            = aws.destination
  name                = "assume_role"
  assume_role_policy  = data.aws_iam_policy_document.assume_role.json
  managed_policy_arns = [data.aws_iam_policy.ec2.arn]
}

resource "aws_security_group" "lambda_sg" {
  name_prefix = "lambda-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lambda_function" "example_lambda" {
  function_name = "example-lambda"
  role          = aws_iam_role.assume_role.arn
  package_type  = "Image"
  image_uri     = var.image_uri
  memory_size   = 1024
  timeout       = 10

  vpc_config {
    subnet_ids         = [aws_subnet.test_subnet_1.id, aws_subnet.test_subnet_2.id]
    security_group_ids = [aws_security_group.lambda_sg.id]
  }
}

output "rds_endpoint" {
  value = aws_db_instance.postgresql.endpoint
}

output "lambda_arn" {
  value = aws_lambda_function.example_lambda.arn
}
