<?php

declare(strict_types=1);

use App\Api\ApiClient;
use App\Api\ApiException;
use App\View\View;

$app = require __DIR__ . '/../src/bootstrap.php';
/** @var ApiClient $apiClient */
$apiClient = $app['apiClient'];

$path = route_path();
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

switch (true) {
    case $path === '/':
        redirect(is_authenticated() ? (is_admin_session() ? '/admin' : '/dashboard') : '/login');

    case $path === '/login' && $method === 'GET':
        View::render('auth/login', [
            'title' => 'Login',
            'form' => [
                'login' => '',
            ],
        ]);
        break;

    case $path === '/login' && $method === 'POST':
        $form = [
            'login' => trim((string) ($_POST['login'] ?? '')),
        ];

        try {
            $response = $apiClient->postJson('/api/auth/login', [
                'login' => $form['login'],
                'password' => (string) ($_POST['password'] ?? ''),
            ]);

            login_user_session([
                'company_api_key' => $response['apiKey'] ?? null,
                'company_id' => $response['companyId'] ?? null,
                'company_name' => $response['companyName'] ?? '',
                'user_id' => $response['userId'] ?? null,
                'user_name' => $response['name'] ?? '',
                'user_email' => $response['email'] ?? '',
                'is_admin' => !empty($response['admin']),
            ]);

            flash('success', 'Login realizado com sucesso.');
            redirect(!empty($response['admin']) ? '/admin' : '/dashboard');
        } catch (ApiException $exception) {
            View::render('auth/login', [
                'title' => 'Login',
                'form' => $form,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/logout':
        logout_user_session();
        flash('success', 'Sessão encerrada.');
        redirect('/login');

    case $path === '/dashboard':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin');
        }
        View::render('dashboard', [
            'title' => 'Dashboard',
            'session' => current_user_session(),
        ]);
        break;

    case $path === '/usuarios' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/usuarios');
        }

        $searchName = trim((string) ($_GET['nome'] ?? ''));
        try {
            $suffix = $searchName !== '' ? '?nome=' . urlencode($searchName) : '';
            $users = $apiClient->getJson('/api/usuarios' . $suffix, current_company_api_key(), current_api_headers());
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => is_array($users) ? $users : [],
                'searchName' => $searchName,
                'form' => ['name' => '', 'email' => ''],
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => [],
                'searchName' => $searchName,
                'form' => ['name' => '', 'email' => ''],
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/usuarios' && $method === 'POST':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/usuarios');
        }

        $form = [
            'name' => trim((string) ($_POST['name'] ?? '')),
            'email' => trim((string) ($_POST['email'] ?? '')),
        ];

        try {
            $apiClient->postJson('/api/usuarios', [
                'name' => $form['name'],
                'email' => $form['email'],
                'password' => (string) ($_POST['password'] ?? ''),
            ], current_company_api_key(), current_api_headers());
            flash('success', 'Usuário criado com sucesso.');
            redirect('/usuarios');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            $users = $apiClient->getJson('/api/usuarios', current_company_api_key(), current_api_headers());
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => is_array($users) ? $users : [],
                'searchName' => '',
                'form' => $form,
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/usuarios/editar' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/usuarios');
        }

        $id = (string) ($_GET['id'] ?? '');
        try {
            $users = $apiClient->getJson('/api/usuarios', current_company_api_key(), current_api_headers());
            $user = find_user_by_id($users, $id);
            if ($user === null) {
                flash('danger', 'Usuário não encontrado.');
                redirect('/usuarios');
            }

            View::render('users/edit', [
                'title' => 'Editar Usuário',
                'user' => $user,
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
            redirect('/usuarios');
        }
        break;

    case $path === '/usuarios/editar' && $method === 'POST':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/usuarios');
        }

        $id = (string) ($_POST['id'] ?? '');
        try {
            $apiClient->putJson('/api/usuarios/' . urlencode($id), [
                'name' => trim((string) ($_POST['name'] ?? '')),
                'email' => trim((string) ($_POST['email'] ?? '')),
                'password' => trim((string) ($_POST['password'] ?? '')),
            ], current_company_api_key(), current_api_headers());
            flash('success', 'Usuário atualizado com sucesso.');
            redirect('/usuarios');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('users/edit', [
                'title' => 'Editar Usuário',
                'user' => [
                    'id' => $id,
                    'name' => trim((string) ($_POST['name'] ?? '')),
                    'email' => trim((string) ($_POST['email'] ?? '')),
                    'companyId' => current_user_session()['company_id'] ?? null,
                    'active' => true,
                ],
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/usuarios/acao' && $method === 'POST':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/usuarios');
        }

        $userId = trim((string) ($_POST['id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';

        try {
            $apiClient->patchJson('/api/usuarios/' . urlencode($userId) . $endpoint, [], current_company_api_key(), current_api_headers());
            flash('success', 'Status do usuário atualizado.');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/usuarios');

    case $path === '/tabelas-frete/upload' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/tabelas-frete/upload');
        }
        View::render('freight/upload', [
            'title' => 'Upload de Tabela de Frete',
            'isAdmin' => false,
        ]);
        break;

    case $path === '/tabelas-frete/upload' && $method === 'POST':
        require_authentication();

        try {
            $result = $apiClient->postMultipart('/api/tabelas-frete/upload-xlsx', [
                'file' => $_FILES['file'] ?? null,
            ], [], current_company_api_key(), current_api_headers());

            View::render('freight/upload', [
                'title' => 'Upload de Tabela de Frete',
                'result' => $result,
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('freight/upload', [
                'title' => 'Upload de Tabela de Frete',
                'error' => $exception->getMessage(),
                'isAdmin' => false,
            ]);
        }
        break;

    case $path === '/tabelas-frete' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/tabelas-frete');
        }
        try {
            $tables = $apiClient->getJson('/api/tabelas-frete', current_company_api_key(), current_api_headers());
            View::render('freight/index', [
                'title' => 'Tabelas de Frete',
                'tables' => is_array($tables) ? $tables : [],
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('freight/index', [
                'title' => 'Tabelas de Frete',
                'tables' => [],
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/tabelas-frete/acao' && $method === 'POST':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/tabelas-frete');
        }
        $tableId = trim((string) ($_POST['id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        try {
            $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';
            $apiClient->patchJson('/api/tabelas-frete/' . urlencode($tableId) . $endpoint, [], current_company_api_key(), current_api_headers());
            flash('success', 'Status da tabela atualizado.');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/tabelas-frete');

    case $path === '/cotacoes' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/cotacoes');
        }
        View::render('quotes/form', [
            'title' => 'Cotações',
            'isAdmin' => false,
            'form' => default_quote_form(),
        ]);
        break;

    case $path === '/cotacoes' && $method === 'POST':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/cotacoes');
        }

        $form = default_quote_form([
            'uf_origem' => trim((string) ($_POST['uf_origem'] ?? '')),
            'uf_destino' => trim((string) ($_POST['uf_destino'] ?? '')),
            'peso' => trim((string) ($_POST['peso'] ?? '')),
            'valor_nf' => trim((string) ($_POST['valor_nf'] ?? '')),
        ]);

        try {
            $result = $apiClient->postJson('/api/cotacoes', [
                'ufOrigem' => strtoupper($form['uf_origem']),
                'ufDestino' => strtoupper($form['uf_destino']),
                'peso' => (float) $form['peso'],
                'valorNF' => (float) $form['valor_nf'],
            ], current_company_api_key(), current_api_headers());

            View::render('quotes/form', [
                'title' => 'Cotações',
                'isAdmin' => false,
                'form' => $form,
                'result' => $result,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('quotes/form', [
                'title' => 'Cotações',
                'isAdmin' => false,
                'form' => $form,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/previsoes' && $method === 'GET':
        require_authentication();
        View::render('ml/predict', [
            'title' => 'Previsões',
            'isAdmin' => is_admin_session(),
            'form' => default_prediction_form(),
        ]);
        break;

    case $path === '/previsoes' && $method === 'POST':
        require_authentication();

        $form = default_prediction_form([
            'peso_total_bruto' => trim((string) ($_POST['peso_total_bruto'] ?? '')),
            'metro_cubico' => trim((string) ($_POST['metro_cubico'] ?? '')),
            'valor_nf' => trim((string) ($_POST['valor_nf'] ?? '')),
            'volume_nf' => trim((string) ($_POST['volume_nf'] ?? '')),
            'tipo_frete_nf' => trim((string) ($_POST['tipo_frete_nf'] ?? 'CIF')),
            'via_transporte' => trim((string) ($_POST['via_transporte'] ?? 'Rodoviário')),
            'uf_emitente_nf' => trim((string) ($_POST['uf_emitente_nf'] ?? '')),
            'uf_destinatario_nf' => trim((string) ($_POST['uf_destinatario_nf'] ?? '')),
        ]);

        try {
            $result = $apiClient->postJson('/api/ml/predict', [
                'pesoTotalBruto' => (float) $form['peso_total_bruto'],
                'metroCubico' => (float) $form['metro_cubico'],
                'valorNF' => (float) $form['valor_nf'],
                'volumeNF' => (int) $form['volume_nf'],
                'tipoFreteNF' => $form['tipo_frete_nf'],
                'viaTransporte' => $form['via_transporte'],
                'ufEmitenteNF' => strtoupper($form['uf_emitente_nf']),
                'ufDestinatarioNF' => strtoupper($form['uf_destinatario_nf']),
            ], current_company_api_key(), current_api_headers());

            View::render('ml/predict', [
                'title' => 'Previsões',
                'isAdmin' => is_admin_session(),
                'form' => $form,
                'result' => $result,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('ml/predict', [
                'title' => 'Previsões',
                'isAdmin' => is_admin_session(),
                'form' => $form,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/ml/retrain' && $method === 'GET':
        require_authentication();
        View::render('ml/retrain', [
            'title' => 'Treinamento do Modelo',
            'isAdmin' => is_admin_session(),
        ]);
        break;

    case $path === '/ml/retrain' && $method === 'POST':
        require_authentication();

        try {
            $result = $apiClient->postMultipart('/api/ml/retrain/upload-xlsx', [
                'file' => $_FILES['file'] ?? null,
            ], [], current_company_api_key(), current_api_headers());

            View::render('ml/retrain', [
                'title' => 'Treinamento do Modelo',
                'result' => $result,
                'isAdmin' => is_admin_session(),
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('ml/retrain', [
                'title' => 'Treinamento do Modelo',
                'error' => $exception->getMessage(),
                'isAdmin' => is_admin_session(),
            ]);
        }
        break;

    case $path === '/apikeys' && $method === 'GET':
        require_authentication();
        if (is_admin_session()) {
            redirect('/admin/apikeys');
        }

        try {
            $apiKeys = $apiClient->getJson('/api/external-apikeys', current_company_api_key(), current_api_headers());
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => is_array($apiKeys) ? $apiKeys : [],
                'form' => ['custom_name' => ''],
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => [],
                'form' => ['custom_name' => ''],
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/apikeys' && $method === 'POST':
        require_authentication();
        $form = [
            'custom_name' => trim((string) ($_POST['custom_name'] ?? '')),
        ];

        try {
            $apiClient->postJson('/api/external-apikeys', [
                'customName' => $form['custom_name'],
            ], current_company_api_key(), current_api_headers());

            $apiKeys = $apiClient->getJson('/api/external-apikeys', current_company_api_key(), current_api_headers());
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => is_array($apiKeys) ? $apiKeys : [],
                'form' => ['custom_name' => ''],
                'success' => 'Nova API key criada com sucesso.',
                'isAdmin' => false,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);

            $apiKeys = [];
            try {
                $apiKeys = $apiClient->getJson('/api/external-apikeys', current_company_api_key(), current_api_headers());
            } catch (ApiException $listingException) {
                guard_authenticated_api_exception($listingException);
            }

            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => is_array($apiKeys) ? $apiKeys : [],
                'form' => $form,
                'isAdmin' => false,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin' && $method === 'GET':
        require_admin();
        View::render('admin/dashboard', [
            'title' => 'Admin',
            'session' => current_user_session(),
        ]);
        break;

    case $path === '/admin/empresas' && $method === 'GET':
        require_admin();
        try {
            $companies = $apiClient->getJson('/api/empresas', current_company_api_key());
            View::render('admin/companies', [
                'title' => 'Empresas',
                'companies' => is_array($companies) ? $companies : [],
                'form' => default_company_form(),
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('admin/companies', [
                'title' => 'Empresas',
                'companies' => [],
                'form' => default_company_form(),
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/empresas' && $method === 'POST':
        require_admin();
        $form = default_company_form([
            'name' => trim((string) ($_POST['name'] ?? '')),
            'social_name' => trim((string) ($_POST['social_name'] ?? '')),
            'document' => trim((string) ($_POST['document'] ?? '')),
        ]);
        try {
            $apiClient->postJson('/api/empresas', [
                'name' => $form['name'],
                'socialName' => $form['social_name'],
                'document' => $form['document'],
            ], current_company_api_key());
            flash('success', 'Empresa criada com sucesso.');
            redirect('/admin/empresas');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            $companies = [];
            try {
                $companies = $apiClient->getJson('/api/empresas', current_company_api_key());
            } catch (ApiException $listingException) {
                guard_authenticated_api_exception($listingException);
            }
            View::render('admin/companies', [
                'title' => 'Empresas',
                'companies' => is_array($companies) ? $companies : [],
                'form' => $form,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/empresas/editar' && $method === 'GET':
        require_admin();
        $id = (string) ($_GET['id'] ?? '');
        try {
            $company = $apiClient->getJson('/api/empresas/' . urlencode($id), current_company_api_key());
            View::render('admin/company_edit', [
                'title' => 'Editar Empresa',
                'company' => $company,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
            redirect('/admin/empresas');
        }
        break;

    case $path === '/admin/empresas/editar' && $method === 'POST':
        require_admin();
        $id = (string) ($_POST['id'] ?? '');
        $company = [
            'id' => $id,
            'name' => trim((string) ($_POST['name'] ?? '')),
            'socialName' => trim((string) ($_POST['social_name'] ?? '')),
            'document' => trim((string) ($_POST['document'] ?? '')),
        ];
        try {
            $apiClient->putJson('/api/empresas/' . urlencode($id), [
                'name' => $company['name'],
                'socialName' => $company['socialName'],
                'document' => $company['document'],
            ], current_company_api_key());
            flash('success', 'Empresa atualizada com sucesso.');
            redirect('/admin/empresas');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('admin/company_edit', [
                'title' => 'Editar Empresa',
                'company' => $company,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/empresas/acao' && $method === 'POST':
        require_admin();
        $companyId = trim((string) ($_POST['id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        try {
            if ($action === 'excluir') {
                $apiClient->delete('/api/empresas/' . urlencode($companyId), current_company_api_key());
                flash('success', 'Empresa excluída com sucesso.');
            } else {
                $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';
                $apiClient->patchJson('/api/empresas/' . urlencode($companyId) . $endpoint, [], current_company_api_key());
                flash('success', 'Status da empresa atualizado.');
            }
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/admin/empresas');

    case $path === '/admin/usuarios' && $method === 'GET':
        require_admin();
        $searchName = trim((string) ($_GET['nome'] ?? ''));
        $companyId = trim((string) ($_GET['company_id'] ?? ''));
        $query = [];
        if ($searchName !== '') {
            $query['nome'] = $searchName;
        }
        if ($companyId !== '') {
            $query['companyId'] = $companyId;
        }
        $suffix = $query !== [] ? '?' . http_build_query($query) : '';
        try {
            $users = $apiClient->getJson('/api/admin/usuarios' . $suffix, current_company_api_key());
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => is_array($users) ? $users : [],
                'searchName' => $searchName,
                'form' => ['name' => '', 'email' => '', 'company_id' => $companyId],
                'isAdmin' => true,
                'companyIdFilter' => $companyId,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => [],
                'searchName' => $searchName,
                'form' => ['name' => '', 'email' => '', 'company_id' => $companyId],
                'isAdmin' => true,
                'companyIdFilter' => $companyId,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/usuarios' && $method === 'POST':
        require_admin();
        $form = [
            'name' => trim((string) ($_POST['name'] ?? '')),
            'email' => trim((string) ($_POST['email'] ?? '')),
            'company_id' => trim((string) ($_POST['company_id'] ?? '')),
        ];
        try {
            $apiClient->postJson('/api/admin/usuarios', [
                'name' => $form['name'],
                'email' => $form['email'],
                'password' => (string) ($_POST['password'] ?? ''),
                'companyId' => $form['company_id'] === '' ? null : (int) $form['company_id'],
            ], current_company_api_key());
            flash('success', 'Usuário criado com sucesso.');
            redirect('/admin/usuarios' . ($form['company_id'] !== '' ? '?company_id=' . urlencode($form['company_id']) : ''));
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            $suffix = $form['company_id'] !== '' ? '?companyId=' . urlencode($form['company_id']) : '';
            $users = $apiClient->getJson('/api/admin/usuarios' . $suffix, current_company_api_key());
            View::render('users/index', [
                'title' => 'Usuários',
                'users' => is_array($users) ? $users : [],
                'searchName' => '',
                'form' => $form,
                'isAdmin' => true,
                'companyIdFilter' => $form['company_id'],
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/usuarios/editar' && $method === 'GET':
        require_admin();
        $id = (string) ($_GET['id'] ?? '');
        try {
            $users = $apiClient->getJson('/api/admin/usuarios', current_company_api_key());
            $user = find_user_by_id($users, $id);
            if ($user === null) {
                flash('danger', 'Usuário não encontrado.');
                redirect('/admin/usuarios');
            }
            View::render('users/edit', [
                'title' => 'Editar Usuário',
                'user' => $user,
                'isAdmin' => true,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
            redirect('/admin/usuarios');
        }
        break;

    case $path === '/admin/usuarios/editar' && $method === 'POST':
        require_admin();
        $id = (string) ($_POST['id'] ?? '');
        $companyId = trim((string) ($_POST['company_id'] ?? ''));
        try {
            $apiClient->putJson('/api/admin/usuarios/' . urlencode($id), [
                'name' => trim((string) ($_POST['name'] ?? '')),
                'email' => trim((string) ($_POST['email'] ?? '')),
                'password' => trim((string) ($_POST['password'] ?? '')),
                'companyId' => $companyId === '' ? null : (int) $companyId,
            ], current_company_api_key());
            flash('success', 'Usuário atualizado com sucesso.');
            redirect('/admin/usuarios');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('users/edit', [
                'title' => 'Editar Usuário',
                'user' => [
                    'id' => $id,
                    'name' => trim((string) ($_POST['name'] ?? '')),
                    'email' => trim((string) ($_POST['email'] ?? '')),
                    'companyId' => $companyId,
                    'active' => true,
                ],
                'isAdmin' => true,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/usuarios/acao' && $method === 'POST':
        require_admin();
        $userId = trim((string) ($_POST['id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        try {
            if ($action === 'excluir') {
                $apiClient->delete('/api/admin/usuarios/' . urlencode($userId), current_company_api_key());
                flash('success', 'Usuário excluído com sucesso.');
            } else {
                $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';
                $apiClient->patchJson('/api/admin/usuarios/' . urlencode($userId) . $endpoint, [], current_company_api_key());
                flash('success', 'Status do usuário atualizado.');
            }
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/admin/usuarios');

    case $path === '/admin/tabelas-frete/upload' && $method === 'GET':
        require_admin();
        View::render('freight/upload', [
            'title' => 'Upload de Tabela de Frete',
            'isAdmin' => true,
            'form' => ['company_id' => ''],
        ]);
        break;

    case $path === '/admin/tabelas-frete/upload' && $method === 'POST':
        require_admin();
        $companyId = trim((string) ($_POST['company_id'] ?? ''));
        try {
            $result = $apiClient->postMultipart('/api/admin/tabelas-frete/upload-xlsx', [
                'file' => $_FILES['file'] ?? null,
            ], [
                'companyId' => $companyId,
            ], current_company_api_key());
            View::render('freight/upload', [
                'title' => 'Upload de Tabela de Frete',
                'result' => $result,
                'isAdmin' => true,
                'form' => ['company_id' => $companyId],
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('freight/upload', [
                'title' => 'Upload de Tabela de Frete',
                'error' => $exception->getMessage(),
                'isAdmin' => true,
                'form' => ['company_id' => $companyId],
            ]);
        }
        break;

    case $path === '/admin/tabelas-frete' && $method === 'GET':
        require_admin();
        $companyId = trim((string) ($_GET['company_id'] ?? ''));
        $suffix = $companyId !== '' ? '?companyId=' . urlencode($companyId) : '';
        try {
            $tables = $apiClient->getJson('/api/admin/tabelas-frete' . $suffix, current_company_api_key());
            View::render('freight/index', [
                'title' => 'Tabelas de Frete',
                'tables' => is_array($tables) ? $tables : [],
                'isAdmin' => true,
                'form' => ['company_id' => $companyId],
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('freight/index', [
                'title' => 'Tabelas de Frete',
                'tables' => [],
                'isAdmin' => true,
                'form' => ['company_id' => $companyId],
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/tabelas-frete/acao' && $method === 'POST':
        require_admin();
        $tableId = trim((string) ($_POST['id'] ?? ''));
        $companyId = trim((string) ($_POST['company_id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        try {
            $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';
            $apiClient->patchJson('/api/admin/tabelas-frete/' . urlencode($tableId) . $endpoint, [], current_company_api_key());
            flash('success', 'Status da tabela atualizado.');
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/admin/tabelas-frete' . ($companyId !== '' ? '?company_id=' . urlencode($companyId) : ''));

    case $path === '/admin/cotacoes' && $method === 'GET':
        require_admin();
        View::render('quotes/form', [
            'title' => 'Cotações',
            'isAdmin' => true,
            'form' => default_quote_form(['company_id' => '']),
        ]);
        break;

    case $path === '/admin/cotacoes' && $method === 'POST':
        require_admin();
        $form = default_quote_form([
            'company_id' => trim((string) ($_POST['company_id'] ?? '')),
            'uf_origem' => trim((string) ($_POST['uf_origem'] ?? '')),
            'uf_destino' => trim((string) ($_POST['uf_destino'] ?? '')),
            'peso' => trim((string) ($_POST['peso'] ?? '')),
            'valor_nf' => trim((string) ($_POST['valor_nf'] ?? '')),
        ]);

        try {
            $result = $apiClient->postJson('/api/admin/cotacoes?companyId=' . urlencode($form['company_id']), [
                'ufOrigem' => strtoupper($form['uf_origem']),
                'ufDestino' => strtoupper($form['uf_destino']),
                'peso' => (float) $form['peso'],
                'valorNF' => (float) $form['valor_nf'],
            ], current_company_api_key());

            View::render('quotes/form', [
                'title' => 'Cotações',
                'isAdmin' => true,
                'form' => $form,
                'result' => $result,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('quotes/form', [
                'title' => 'Cotações',
                'isAdmin' => true,
                'form' => $form,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/apikeys' && $method === 'GET':
        require_admin();
        $companyId = trim((string) ($_GET['company_id'] ?? ''));
        $suffix = $companyId !== '' ? '?companyId=' . urlencode($companyId) : '';
        try {
            $apiKeys = $apiClient->getJson('/api/admin/external-apikeys' . $suffix, current_company_api_key());
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => is_array($apiKeys) ? $apiKeys : [],
                'form' => ['custom_name' => '', 'company_id' => $companyId],
                'isAdmin' => true,
            ]);
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => [],
                'form' => ['custom_name' => '', 'company_id' => $companyId],
                'isAdmin' => true,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/apikeys' && $method === 'POST':
        require_admin();
        $form = [
            'custom_name' => trim((string) ($_POST['custom_name'] ?? '')),
            'company_id' => trim((string) ($_POST['company_id'] ?? '')),
        ];
        try {
            $apiClient->postJson('/api/admin/external-apikeys', [
                'customName' => $form['custom_name'],
                'companyId' => $form['company_id'] === '' ? null : (int) $form['company_id'],
            ], current_company_api_key());
            flash('success', 'Nova API key criada com sucesso.');
            redirect('/admin/apikeys' . ($form['company_id'] !== '' ? '?company_id=' . urlencode($form['company_id']) : ''));
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            $suffix = $form['company_id'] !== '' ? '?companyId=' . urlencode($form['company_id']) : '';
            $apiKeys = $apiClient->getJson('/api/admin/external-apikeys' . $suffix, current_company_api_key());
            View::render('apikeys/index', [
                'title' => 'API Keys Externas',
                'apiKeys' => is_array($apiKeys) ? $apiKeys : [],
                'form' => $form,
                'isAdmin' => true,
                'error' => $exception->getMessage(),
            ]);
        }
        break;

    case $path === '/admin/apikeys/acao' && $method === 'POST':
        require_admin();
        $apiKeyId = trim((string) ($_POST['id'] ?? ''));
        $companyId = trim((string) ($_POST['company_id'] ?? ''));
        $action = trim((string) ($_POST['action'] ?? ''));
        try {
            if ($action === 'excluir') {
                $apiClient->delete('/api/admin/external-apikeys/' . urlencode($apiKeyId), current_company_api_key());
                flash('success', 'Acesso externo excluído com sucesso.');
            } else {
                $endpoint = $action === 'ativar' ? '/ativar' : '/desativar';
                $apiClient->patchJson('/api/admin/external-apikeys/' . urlencode($apiKeyId) . $endpoint, [], current_company_api_key());
                flash('success', 'Status do acesso externo atualizado.');
            }
        } catch (ApiException $exception) {
            guard_authenticated_api_exception($exception);
            flash('danger', $exception->getMessage());
        }
        redirect('/admin/apikeys' . ($companyId !== '' ? '?company_id=' . urlencode($companyId) : ''));

    default:
        http_response_code(404);
        View::render('errors/404', [
            'title' => 'Página não encontrada',
        ]);
        break;
}

function find_user_by_id(array $users, string $id): ?array
{
    foreach ($users as $user) {
        if ((string) ($user['id'] ?? '') === $id) {
            return is_array($user) ? $user : null;
        }
    }
    return null;
}

function default_quote_form(array $overrides = []): array
{
    return array_merge([
        'company_id' => '',
        'uf_origem' => '',
        'uf_destino' => '',
        'peso' => '',
        'valor_nf' => '',
    ], $overrides);
}

function default_prediction_form(array $overrides = []): array
{
    return array_merge([
        'peso_total_bruto' => '',
        'metro_cubico' => '',
        'valor_nf' => '',
        'volume_nf' => '',
        'tipo_frete_nf' => 'CIF',
        'via_transporte' => 'Rodoviário',
        'uf_emitente_nf' => '',
        'uf_destinatario_nf' => '',
    ], $overrides);
}

function default_company_form(array $overrides = []): array
{
    return array_merge([
        'name' => '',
        'social_name' => '',
        'document' => '',
    ], $overrides);
}
