import os
import re

# 1. Update POM
pom_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/pom.xml'
with open(pom_path, 'r') as f:
    pom_content = f.read()

if 'springdoc-openapi-starter-webmvc-ui' not in pom_content:
    pom_content = re.sub(
        r'(<artifactId>spring-boot-starter-webmvc</artifactId>\s*</dependency>)',
        r'\1\n\t\t<dependency>\n\t\t\t<groupId>org.springdoc</groupId>\n\t\t\t<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\n\t\t\t<version>2.5.0</version>\n\t\t</dependency>',
        pom_content
    )
    with open(pom_path, 'w') as f:
        f.write(pom_content)

# 2. Add Swagger Annotations to Controllers
controller_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow/controller/'
for filename in os.listdir(controller_path):
    if filename.endswith('.java'):
        filepath = os.path.join(controller_path, filename)
        with open(filepath, 'r') as f:
            content = f.read()

        new_content = content
        
        # Add imports
        if 'io.swagger.v3.oas.annotations' not in new_content:
            imports = "import io.swagger.v3.oas.annotations.Operation;\nimport io.swagger.v3.oas.annotations.tags.Tag;\n"
            match = re.search(r'(import .*?;[\r\n]+)(?!import )', new_content)
            if match:
                new_content = new_content[:match.end()] + imports + new_content[match.end():]

        # Add @Tag
        if '@Tag' not in new_content:
            tag_name = filename.replace('Controller.java', ' API')
            new_content = re.sub(r'(@RestController)', f'@Tag(name = "{tag_name}", description = "Operations related to {tag_name.lower()}")\n\\1', new_content)

        # Add @Operation
        # Let's find all mapping annotations and add @Operation before them if missing
        def repl_mapping(m):
            mapping = m.group(1)
            method = m.group(2)
            operation = f'@Operation(summary = "{mapping.split("(")[0]} operation for {method}")'
            return f"{operation}\n    {mapping}\n    public {method}"
        
        new_content = re.sub(r'(@(?:Get|Post|Put|Delete|Patch)Mapping.*?)\s+public\s+([A-Za-z0-9<>?]+)', repl_mapping, new_content)

        with open(filepath, 'w') as f:
            f.write(new_content)

# 3. Add @Slf4j to Services
service_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow/service/'
for filename in os.listdir(service_path):
    if filename.endswith('.java') and 'EmailService' not in filename: # skip EmailService because it manually configures logger
        filepath = os.path.join(service_path, filename)
        with open(filepath, 'r') as f:
            content = f.read()

        new_content = content
        if '@Slf4j' not in new_content:
            imports = "import lombok.extern.slf4j.Slf4j;\n"
            match = re.search(r'(import .*?;[\r\n]+)(?!import )', new_content)
            if match:
                new_content = new_content[:match.end()] + imports + new_content[match.end():]
            
            new_content = re.sub(r'(@Service)', r'@Slf4j\n\1', new_content)
        
        with open(filepath, 'w') as f:
            f.write(new_content)

print("POM, Controllers, and Services updated successfully.")
