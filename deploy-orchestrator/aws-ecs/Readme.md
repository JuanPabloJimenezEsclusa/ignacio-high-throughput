# AWS ECS

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Usage](#-usage)
  • [Links](#-links)

Based on `AWS Cloud Provider`

## ⚙️ Dependencies

---

* [Docker ~29](https://docs.docker.com/engine/release-notes/28/)
* [AWS CLI ~2.32](https://github.com/aws/aws-cli/blob/v2/CHANGELOG.rst)

## 🏗️ Architecture

---

Basic deployment prioritizing simplification of the architecture and leveraging the AWS free tier. Designed as a development environment.

![High Throughput AWS Cloudformation](./images/high-throughput-aws-cf-diagram.svg)

## 🚀 Usage

---

❗ This infrastructure incurs costs. Avoid keeping it running if it's not in use.

```bash
# Init container infrastructure
./init-aws-stack.sh buildProjects=true
```

```bash
# Delete container infrastructure
./delete-aws-stack.sh removeImages=true
```

## 🔗 Links

---

* AWS UI
  1. [AWS CloudFormation](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks?filteringText=&filteringStatus=active&viewNested=true)
  2. [AWS Certificate Manager (ACM)](https://eu-west-1.console.aws.amazon.com/acm/home?region=eu-west-1#/certificates/list)
  3. [AWS ECR](https://eu-west-1.console.aws.amazon.com/ecr/public-registry/repositories?region=eu-west-1)
  4. [AWS CloudWatch](https://eu-west-1.console.aws.amazon.com/cloudwatch/home?region=eu-west-1#logsV2:log-groups)
  5. [AWS ECS Cluster](https://eu-west-1.console.aws.amazon.com/ecs/v2/clusters/high-throughput-cluster/services?region=eu-west-1) 💰
  6. [AWS VPC](https://eu-west-1.console.aws.amazon.com/vpcconsole/home?region=eu-west-1#vpcs) 💰
  7. [AWS EC2 Load Balancer](https://eu-west-1.console.aws.amazon.com/ec2/home?region=eu-west-1#LoadBalancers) 💰
  8. [AWS Route 53](https://eu-west-1.console.aws.amazon.com/route53/v2/hostedzones?region=eu-west-1) 💰
