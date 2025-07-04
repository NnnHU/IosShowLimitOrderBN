# Project Description

This project is an Apple iPhone application designed to acquire real-time market depth data, monitor buy/sell ratios and large orders, generate text and chart outputs, and finally push the results to specified channels.

## Project Background

- **Application Type**: Apple iPhone APP
- **Technical Reference**: This project refers to the Python project located in the `otherCODE` directory.
- **Interface Design**: The APP's interface style should refer to the message style sent to Discord by the aforementioned Python project.

## Application Architecture Design

### Core Modules
- **Data Acquisition Module**: Real-time acquisition of market depth data
- **Analysis Module**: Calculation of buy/sell ratios and large order monitoring
- **Output Module**: Generation of text and chart outputs
- **Push Module**: Sending results to specified channels

### Technology Stack
- **Frontend**: SwiftUI
- **Data Layer**: CoreData + Alamofire
- **Analysis Layer**: Custom algorithms
- **Push Layer**: Discord API integration

## Development Guidelines

- **Design First**: Before starting any coding work, a detailed design document must be written. The design document should include application architecture, UI/UX sketches, data flow, and technical implementation ideas for key features.
- **Task Tracking**: All development steps, whether large or small tasks, must be recorded and tracked in the `TODO.md` file. This helps maintain clarity and transparency in the workflow.
