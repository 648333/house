import os

files_to_fix = [
    r'e:\AI\trae\trae project\daojishi\frontend\src\App.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\HomeView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\LoginView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\RegisterView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\PropertyListView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\AdminDashboard.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\AgentDashboard.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\PropertyFormView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\PropertyDetailView.vue',
    r'e:\AI\trae\trae project\daojishi\frontend\src\views\UserProfileView.vue'
]

for file_path in files_to_fix:
    try:
        # Try reading with utf-8 first
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            print(f"Read {file_path} as UTF-8 successfully.")
        except UnicodeDecodeError:
            # If failed, try reading with gbk (common in Windows)
            with open(file_path, 'r', encoding='gbk') as f:
                content = f.read()
            print(f"Read {file_path} as GBK successfully.")
        
        # Write back as utf-8
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Converted {file_path} to UTF-8.")
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
