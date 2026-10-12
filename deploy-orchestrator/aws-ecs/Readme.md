---
layout: default
title: orchestrator (ECS) (AWS)
---

# orchestrator (ECS) (AWS)

> • [Dependencies](#dependencies)
  • [Architecture](#architecture)
  • [Usage](#usage)
  • [Links](#links)

This deployment uses the AWS Cloud provider.

## Dependencies

* [Docker ~29](https://docs.docker.com/engine/release-notes/29/)
* [AWS CLI ~2.34](https://github.com/aws/aws-cli/blob/v2/CHANGELOG.rst)

## Architecture

A basic deployment for development. It keeps the architecture simple and stays close to the AWS free tier.

![High Throughput AWS Cloudformation](./images/high-throughput-aws-cf-diagram.svg)

## Usage

NOTE: The infrastructure costs money. Delete it when you no longer need it.

NOTE: The script installs the AWS CLI, so it needs `sudo`.

```bash
# Create the container infrastructure
./init-aws-stack.sh buildProjects=true
```

```bash
# Delete the container infrastructure
./delete-aws-stack.sh removeImages=true
```

## Links

The resources below are billable except where noted.

1. [AWS CloudFormation](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks?filteringText=&filteringStatus=active&viewNested=true)
2. [AWS Certificate Manager (ACM)](https://eu-west-1.console.aws.amazon.com/acm/home?region=eu-west-1#/certificates/list)
3. [AWS ECR](https://eu-west-1.console.aws.amazon.com/ecr/public-registry/repositories?region=eu-west-1)
4. [AWS CloudWatch](https://eu-west-1.console.aws.amazon.com/cloudwatch/home?region=eu-west-1#logsV2:log-groups)
5. [AWS ECS Cluster](https://eu-west-1.console.aws.amazon.com/ecs/v2/clusters/high-throughput-cluster/services?region=eu-west-1)
6. [AWS VPC](https://eu-west-1.console.aws.amazon.com/vpcconsole/home?region=eu-west-1#vpcs)
7. [AWS EC2 Load Balancer](https://eu-west-1.console.aws.amazon.com/ec2/home?region=eu-west-1#LoadBalancers)
8. [AWS Route 53](https://eu-west-1.console.aws.amazon.com/route53/v2/hostedzones?region=eu-west-1)
9. [AWS Cloud Map](https://eu-west-1.console.aws.amazon.com/cloudmap/home/namespaces?region=eu-west-1)
