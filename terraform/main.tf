provider "aws" {
  region = "eu-central-1"  # Replace with your desired region
}

# Create a new VPC
resource "aws_vpc" "test_vpc" {
  cidr_block          = "10.0.0.0/16"
  enable_dns_hostnames = true
}

# Create two subnets in different Availability Zones
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

# Create a Security Group for instances in the public subnet
resource "aws_security_group" "public_sg" {
  name_prefix = "public-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  # Allow HTTP access from anywhere
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create a Security Group for the application servers
resource "aws_security_group" "application_sg" {
  name_prefix = "app-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  # Allow inbound traffic from specific CIDR blocks (e.g., application servers, bastion hosts, etc.)
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.3.0/24"]  # Replace with the CIDR block of your application servers
  }

  # Allow outbound traffic to the internet
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create a Security Group for the RDS instance
resource "aws_security_group" "rds_sg" {
  name_prefix = "rds-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  # Allow inbound traffic from the application or bastion hosts
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.application_sg.id]
  }

  # Allow outbound traffic to the internet
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create an RDS subnet group
resource "aws_db_subnet_group" "example" {
  name       = "postgres-subnet-group"  # Change this name to a unique value
  subnet_ids = [aws_subnet.test_subnet_1.id, aws_subnet.test_subnet_2.id]
}

# Create an RDS PostgreSQL instance
resource "aws_db_instance" "postgresql" {
  identifier             = "example-db"
  engine                 = "postgres"
  engine_version         = "15.3"
  instance_class         = "db.t3.micro"
  username               = var.postgre_id
  password               = var.postgre_pw
  allocated_storage      = 20
  publicly_accessible    = false  # Keep the RDS instance private within the VPC
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.example.name

  # Add any other necessary RDS settings
}

# Create a random string to ensure a unique IAM role name
resource "random_string" "random_suffix" {
  length  = 8
  special = false
}

# Create an IAM role for the Lambda function
resource "aws_iam_role" "lambda_role" {
  name = "lambda-apisubnetgroup-${random_string.random_suffix.result}"  # Change this name to a unique value

  # Attach necessary policies for the Lambda function
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Attach a policy with necessary EC2 permissions to the IAM role
resource "aws_iam_policy" "lambda_ec2_policy" {
  name        = "LambdaEC2Permissions"
  description = "Policy granting EC2 permissions for Lambda function"
  policy      = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "ec2:CreateNetworkInterface"
        Resource = "*"
      }
      # Add more permissions here if needed for your Lambda function
    ]
  })
}

# Attach the policy to the IAM role
resource "aws_iam_role_policy_attachment" "lambda_role_attachment" {
  policy_arn = aws_iam_policy.lambda_ec2_policy.arn
  role       = aws_iam_role.lambda_role.name
}

# Create a security group for the Lambda function
resource "aws_security_group" "lambda_sg" {
  name_prefix = "lambda-sg-"
  vpc_id      = aws_vpc.test_vpc.id

  # Allow outbound internet access for the Lambda function
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Create a Lambda function
resource "aws_lambda_function" "example_lambda" {
  function_name = "example-lambda"
  role          = aws_iam_role.lambda_role.arn
  package_type  = "Image"
  image_uri     = var.image_uri  # Replace with your ECR image URI
  memory_size   = 1024           # Set the memory size for the Lambda function
  timeout       = 10             # Set the timeout in seconds for the Lambda function

  vpc_config {
    subnet_ids         = [aws_subnet.test_subnet_1.id, aws_subnet.test_subnet_2.id]
    security_group_ids = [aws_security_group.lambda_sg.id]
  }

  # Add any other necessary Lambda function settings
}

# Output the RDS endpoint and the Lambda function ARN
output "rds_endpoint" {
  value = aws_db_instance.postgresql.endpoint
}

output "lambda_arn" {
  value = aws_lambda_function.example_lambda.arn
}
