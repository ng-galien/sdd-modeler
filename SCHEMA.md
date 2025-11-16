# SDD Model JSON Schema

Ce fichier contient le schéma JSON pour valider les fichiers YAML/JSON de modèles SDD (State-Driven Design).

## 🎯 Utilisation

### VS Code

1. **Méthode 1 - Configuration workspace** (recommandée)

Créez/modifiez `.vscode/settings.json` dans votre projet :

```json
{
  "yaml.schemas": {
    "https://raw.githubusercontent.com/votre-org/sdd-modeler/main/sdd-model-schema.json": [
      "*.sdd.yaml",
      "*.sdd.yml"
    ]
  },
  "json.schemas": [
    {
      "fileMatch": ["*.sdd.json"],
      "url": "https://raw.githubusercontent.com/votre-org/sdd-modeler/main/sdd-model-schema.json"
    }
  ]
}
```

1. **Méthode 1 - Tout en un**

Ajoutez en haut de vos fichiers SDD YAML :

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/votre-org/sdd-modeler/main/sdd-model-schema.json

version: "0.1"
name: "my-sdd-model"
# ... votre modèle SDD
```

### IntelliJ IDEA

1. Allez dans **File** → **Settings** → **Languages & Frameworks** → **Schemas and DTDs** → **JSON Schema Mappings**
2. Cliquez sur **+** pour ajouter un nouveau mapping
3. **Schema file or URL**: `https://raw.githubusercontent.com/votre-org/sdd-modeler/main/sdd-model-schema.json`
4. **Schema version**: JSON Schema version 2020-12
5. Dans **File path pattern**, ajoutez : `*.sdd.yaml`, `*.sdd.yml`, `*.sdd.json`

## 🔄 Génération Automatique

Ce schéma est **généré automatiquement** lors du build à partir du code Java :

```bash
# Génère le schéma dans src/main/resources
./gradlew :state-modeler-core:generateJsonSchema

# Copie aussi vers la racine du projet pour GitHub
./gradlew distributeSchema

# Ou génère automatiquement avec le build complet
./gradlew build
```

## 📚 Structure du Schéma

Le schéma valide la structure suivante pour les modèles SDD :

```yaml
version: string        # Version du modèle SDD
name: string          # Nom du modèle
database:             # Configuration de la base de données
  dialect: string     # Dialecte SQL (ex: "postgres")
  schema: string      # Schéma de base de données
entities:             # Entités métier avec leurs états
  # ... structure des entités SDD
```

## 🎁 Avantages

- ✅ **Validation en temps réel** des fichiers YAML/JSON SDD
- 🔍 **Autocomplétion intelligente** dans l'IDE
- 📖 **Documentation intégrée** via les descriptions du schéma
- 🚀 **Développement plus rapide** avec moins d'erreurs

## 🔗 Ressources

- [JSON Schema Specification](https://json-schema.org/)
- [VS Code YAML Extension](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml)
- [IntelliJ JSON Schema Support](https://www.jetbrains.com/help/idea/json.html#ws_json_schema_add_custom)
