import os
import unicodedata

def remove_accents(input_str):
    nfkd_form = unicodedata.normalize('NFKD', input_str)
    return u"".join([c for c in nfkd_form if not unicodedata.combining(c)])

def rename_files(directory):
    for filename in os.listdir(directory):
        new_filename = remove_accents(filename)
        os.rename(os.path.join(directory, filename), os.path.join(directory, new_filename))

# Especifica el directorio donde quieres cambiar los nombres de los archivos
directory_path = '.'  # Directorio actual
rename_files(directory_path)
