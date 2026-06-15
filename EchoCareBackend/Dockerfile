# Use official Node.js lightweight image
FROM node:20-alpine

# Set working directory
WORKDIR /app

# Copy package descriptors
COPY package*.json ./

# Install production dependencies
RUN npm ci --only=production

# Copy server code
COPY server.js .

# Expose backend port
EXPOSE 3000

# Define start execution script
CMD [ "npm", "start" ]
