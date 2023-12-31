variable "login_image_uri" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "register_image_uri" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "refresh_image_uri" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "me_image_uri" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "postgre_id" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "postgre_pw" {
  type = string
  # You can add an optional default value if needed:
  default = ""
}

variable "create_lambda_role" {
  description = "Whether to create the Lambda IAM role"
  type        = bool
  default     = false
}

variable "branch_name" {
  description = "The name of the Git branch"
  type        = string
}