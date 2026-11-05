#!/bin/bash
set -e

echo "=========================================="
echo " Running CloudScale Automated Verification "
echo "=========================================="

# 1. Control Plane Test
echo ""
echo "--> 1. Testing Control Plane (Java/Maven)..."
cd control-plane
mvn test -q
echo "✅ Control Plane tests PASSED"
cd ..

# 2. AI Service Test
if [ -d "ai-service/.venv" ]; then
    echo ""
    echo "--> 2. Testing AI Service (Python/Pytest)..."
    source ai-service/.venv/bin/activate
    pytest ai-service/tests/ -q
    deactivate
    echo "✅ AI Service tests PASSED"
fi

# 3. Node Agent Test
if [ -d "node-agent/.venv" ]; then
    echo ""
    echo "--> 3. Testing Node Agent (Python/Pytest)..."
    source node-agent/.venv/bin/activate
    pytest node-agent/tests/ -q
    deactivate
    echo "✅ Node Agent tests PASSED"
fi

# 4. Angular Console Build
if [ -d "console/node_modules" ]; then
    echo ""
    echo "--> 4. Testing Angular Console (Production Build)..."
    cd console
    npm run build -- --configuration production
    echo "✅ Angular Console build PASSED"
    cd ..
fi

echo ""
echo "=========================================="
echo " All CloudScale checks completed cleanly! "
echo "=========================================="
