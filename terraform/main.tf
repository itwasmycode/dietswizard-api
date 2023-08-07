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

data "aws_db_subnet_group" "existing_subnet_group" {
  name = "postgres-subnet-group" # Change this name to match your existing subnet group name
}

# Correcting the conditional count for aws_db_subnet_group.example
resource "aws_db_subnet_group" "example" {
  count = length(data.aws_db_subnet_group.existing_subnet_group) > 0 ? 0 : 1

  name       = "postgres-subnet-group"
  subnet_ids = [aws_subnet.test_subnet_1.id, aws_subnet.test_subnet_2.id]
}

// user, role, policy, user-group, security-group
resource "aws_iam_role" "lambda_role" {
  name = "lambda-apisubnetgroup"
  count = length(aws_iam_role.lambda_role)> 0 ? 0 :1
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

data "aws_iam_policy" "existing_lambda_ec2_policy" {
  arn = "arn:aws:iam::021114833428:policy/lambda_ec2_policy_test"
}

resource "aws_iam_policy" "lambda_ec2_policy" {
  count = length(data.aws_iam_policy.existing_lambda_ec2_policy)> 0 ? 1 : 0

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "logs:CreateLogGroup"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = "logs:CreateLogStream"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = "logs:PutLogEvents"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = "ec2:CreateNetworkInterface"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = "ec2:DescribeNetworkInterfaces"
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = "ec2:DeleteNetworkInterface"
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_role_attachment" {
  policy_arn = aws_iam_policy.lambda_ec2_policy[0].arn
  role       = aws_iam_role.lambda_role[0].name
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

resource "aws_db_instance" "postgresql" {
  count = length(aws_db_subnet_group.example) > 0 ? 1 : 0

  identifier             = "example-db"
  engine                 = "postgres"
  engine_version         = "15.3"
  instance_class         = "db.t3.micro"
  username               = var.postgre_id
  password               = var.postgre_pw
  allocated_storage      = 20
  publicly_accessible    = false
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.example[0].name
}

resource "aws_lambda_function" "example_lambda" {
  function_name = "example-lambda"
  count         = length(aws_lambda_function.example_lambda)>0 ? 1 : 0
  role          = aws_iam_role.lambda_role.arn
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
  value = length(aws_db_instance.postgresql) > 0 ? aws_db_instance.postgresql[0].endpoint : null
}
