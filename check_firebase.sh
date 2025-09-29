#!/bin/bash

# Script para verificar configuração do Firebase
echo "=== Verificação da Configuração Firebase ==="
echo ""

# Verificar se o google-services.json existe
if [ -f "app/google-services.json" ]; then
    echo "✅ google-services.json encontrado"

    # Verificar se contém valores reais (não placeholder)
    if grep -q "placeholder\|PLACEHOLDER\|dummy\|DUMMY" app/google-services.json; then
        echo "❌ PROBLEMA: google-services.json contém valores placeholder"
        echo "   Você precisa baixar o arquivo REAL do Firebase Console"
        echo "   URL: https://console.firebase.google.com/project/walking-df5d0"
    else
        echo "✅ google-services.json parece conter dados reais"
    fi

    # Verificar project_id
    project_id=$(grep -o '"project_id": "[^"]*"' app/google-services.json | head -1)
    echo "   Project ID: $project_id"
else
    echo "❌ google-services.json não encontrado"
fi

echo ""
echo "=== Próximos Passos ==="
echo "1. Baixe o google-services.json real do Firebase Console"
echo "2. Substitua o arquivo em app/google-services.json"
echo "3. Ative Authentication (Email/senha) no Firebase Console"
echo "4. Crie o Firestore Database no Firebase Console"
echo ""
echo "Firebase Console: https://console.firebase.google.com/project/walking-df5d0"
