provider "aws" {
  region = "eu-central-1"  # Replace with your desired region
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

# Internet gateway
resource "aws_internet_gateway" "internet_gateway_eucentral1" {
  vpc_id = aws_vpc.vpc.id

  tags = {
    Name = "myapp-igw-eucentral1"
  }
}

# Routes tables
# - Public
resource "aws_route_table" "route_table_public_eucentral1a" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gateway_eucentral1.id
  }

  tags = {
    Name = "myapp-rtb-public-eucentral1a"
  }
}
resource "aws_route_table" "route_table_public_eucentral1b" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gateway_eucentral1.id
  }

  tags = {
    Name = "myapp-rtb-public-eucentral1b"
  }
}

# - Private
resource "aws_route_table" "route_table_private_eucentral1a" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gateway_eucentral1a.id
  }

  tags = {
    Name = "myapp-rtb-private-eucentral1a"
  }
}
resource "aws_route_table" "route_table_private_eucentral1b" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gateway_eucentral1b.id
  }

  tags = {
    Name = "myapp-rtb-private-eucentral1b"
  }
}

resource "aws_route_table_association" "route_table_association_public_eucentral1a" {
  subnet_id      = aws_subnet.subnet_public_eucentral1a.id
  route_table_id = aws_route_table.route_table_public_eucentral1a.id
}
resource "aws_route_table_association" "route_table_association_public_eucentral1b" {
  subnet_id      = aws_subnet.subnet_public_eucentral1b.id
  route_table_id = aws_route_table.route_table_public_eucentral1b.id
}

resource "aws_route_table_association" "route_table_association_private_eucentral1a" {
  subnet_id      = aws_subnet.subnet_private_eucentral1a.id
  route_table_id = aws_route_table.route_table_private_eucentral1a.id
}
resource "aws_route_table_association" "route_table_association_private_eucentral1b" {
  subnet_id      = aws_subnet.subnet_private_eucentral1b.id
  route_table_id = aws_route_table.route_table_private_eucentral1b.id
}

resource "aws_eip" "eip_eucentral1a" {
  vpc        = true
  depends_on = [aws_internet_gateway.internet_gateway_eucentral1]
  tags = {
    Name = "myapp-eip-eucentral1a"
  }
}
resource "aws_eip" "eip_eucentral1b" {
  vpc        = true
  depends_on = [aws_internet_gateway.internet_gateway_eucentral1]
  tags = {
    Name = "myapp-eip-eucentral1b"
  }
}

resource "aws_nat_gateway" "nat_gateway_eucentral1a" {
  allocation_id = aws_eip.eip_eucentral1a.id
  subnet_id     = aws_subnet.subnet_public_eucentral1a.id

  tags = {
    Name = "myapp-ngw-eucentral1a"
  }
}
resource "aws_nat_gateway" "nat_gateway_eucentral1b" {
  allocation_id = aws_eip.eip_eucentral1b.id
  subnet_id     = aws_subnet.subnet_public_eucentral1b.id

  tags = {
    Name = "myapp-ngw-eucentral1b"
  }
}
resource "aws_default_network_acl" "default_network_acl" {
  default_network_acl_id = aws_vpc.vpc.default_network_acl_id
  subnet_ids             = [aws_subnet.subnet_public_eucentral1a.id, aws_subnet.subnet_private_eucentral1a.id]

  ingress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  egress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  tags = {
    Name = "myapp-default-network-acl"
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

resource "aws_default_subnet" "def_subnet" {
  availability_zone = "eu-central-1a"
}

resource "aws_default_security_group" "application-test-default-security-group" {
  vpc_id = aws_vpc.vpc.id

  ingress {
    protocol  = -1
    self      = true
    from_port = 0
    to_port   = 0
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    # cidr_blocks = ["127.0.0.1/32"]
  }

  tags = {
    Name = "application-test-default-security-group"
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


resource "aws_lambda_function" "example_lambda_test" {
  function_name = "example-lambda-test"
  role          = aws_iam_role.iam_role.arn
  package_type  = "Image"
  image_uri     = var.image_uri
  memory_size   = 1024
  timeout       = 10

  vpc_config {
    subnet_ids         = [aws_subnet.subnet_private_eucentral1a.id]
    security_group_ids = [aws_default_security_group.application-test-default-security-group.id]
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
  db_subnet_group_name = def_subnet
  skip_final_snapshot    = true
}
