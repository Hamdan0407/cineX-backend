import os
import re

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow/controller/'
for filename in os.listdir(base_path):
    if filename.endswith('.java'):
        filepath = os.path.join(base_path, filename)
        with open(filepath, 'r') as f:
            content = f.read()
        
        new_content = content
        if '@RequestBody' in new_content and not '@Valid' in new_content:
            new_content = re.sub(r'(@RequestBody\s+)([a-zA-Z<>]+)', r'@Valid \1\2', new_content)
            
            # add import jakarta.validation.Valid;
            match = re.search(r'(import .*?;[\r\n]+)(?!import )', new_content)
            if match:
                new_content = new_content[:match.end()] + "import jakarta.validation.Valid;\n" + new_content[match.end():]
        
        if new_content != content:
            with open(filepath, 'w') as f:
                f.write(new_content)
            print(f"Refactored {filename}")
