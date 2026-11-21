(function () {
    'use strict';

    const AUTH_KEY = 'aquadist.auth';
    const USER_KEY = 'aquadist.user';

    const Auth = {
        get() {
            try {
                return JSON.parse(sessionStorage.getItem(AUTH_KEY) || 'null');
            } catch (erro) {
                return null;
            }
        },
        set(email, senha) {
            const header = 'Basic ' + btoa(email + ':' + senha);
            sessionStorage.setItem(AUTH_KEY, JSON.stringify({ email, senha, header }));
        },
        setUser(usuario) {
            sessionStorage.setItem(USER_KEY, JSON.stringify(usuario));
        },
        getUser() {
            try {
                return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null');
            } catch (erro) {
                return null;
            }
        },
        clear() {
            sessionStorage.removeItem(AUTH_KEY);
            sessionStorage.removeItem(USER_KEY);
        }
    };

    async function request(path, options) {
        const config = options || {};
        const headers = new Headers(config.headers || {});
        const auth = Auth.get();

        if (auth) {
            headers.set('Authorization', auth.header);
        }

        let body = config.body;
        if (body !== undefined && body !== null && !(body instanceof FormData)) {
            headers.set('Content-Type', 'application/json');
            body = JSON.stringify(body);
        }

        const resposta = await fetch('/api' + path, {
            method: config.method || 'GET',
            headers,
            body
        });

        const texto = await resposta.text();
        let dados = null;
        if (texto) {
            try {
                dados = JSON.parse(texto);
            } catch (erro) {
                dados = texto;
            }
        }

        if (!resposta.ok) {
            if (resposta.status === 401 && path !== '/auth/me') {
                Auth.clear();
                if (!location.pathname.endsWith('/login.html')) {
                    location.href = '/login.html';
                }
            }

            let mensagem = (dados && (dados.detail || dados.message || dados.title))
                || ('Erro HTTP ' + resposta.status);

            if (dados && dados.campos) {
                const campos = Object.entries(dados.campos)
                    .map(([campo, erro]) => campo + ': ' + erro)
                    .join(' | ');
                mensagem += ' — ' + campos;
            }

            const erro = new Error(mensagem);
            erro.status = resposta.status;
            erro.dados = dados;
            throw erro;
        }

        return dados;
    }

    const API = {
        get(path) {
            return request(path);
        },
        post(path, body) {
            return request(path, { method: 'POST', body: body === undefined ? {} : body });
        },
        put(path, body) {
            return request(path, { method: 'PUT', body });
        },
        delete(path) {
            return request(path, { method: 'DELETE' });
        }
    };

    function homeFor(perfil) {
        if (perfil === 'ADMIN') return '/admin.html';
        if (perfil === 'ENTREGADOR') return '/entregador.html';
        return '/cliente.html';
    }

    async function requireRole(perfilNecessario) {
        const auth = Auth.get();
        if (!auth) {
            location.replace('/login.html');
            throw new Error('Sessao expirada.');
        }

        let usuario;
        try {
            usuario = await API.get('/auth/me');
        } catch (erro) {
            if (erro.status === 401) {
                Auth.clear();
                location.replace('/login.html');
            }
            throw erro;
        }

        Auth.setUser(usuario);

        if (perfilNecessario && usuario.perfil !== perfilNecessario) {
            location.replace(homeFor(usuario.perfil));
            throw new Error('Perfil sem acesso a esta pagina.');
        }

        document.querySelectorAll('[data-user-name]').forEach(el => {
            el.textContent = usuario.nome;
        });
        document.querySelectorAll('[data-user-role]').forEach(el => {
            el.textContent = formatarEnum(usuario.perfil);
        });

        return usuario;
    }

    function money(valor) {
        const numero = Number(valor || 0);
        return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }

    function formatarData(valor) {
        if (!valor) return '—';
        const data = new Date(valor);
        if (Number.isNaN(data.getTime())) return valor;
        return data.toLocaleDateString('pt-BR');
    }

    function formatarDataHora(valor) {
        if (!valor) return '—';
        const data = new Date(valor);
        if (Number.isNaN(data.getTime())) return valor;
        return data.toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    }

    function formatarEnum(valor) {
        if (!valor) return '—';
        return String(valor).toLowerCase()
            .split('_')
            .map(p => p.charAt(0).toUpperCase() + p.slice(1))
            .join(' ');
    }

    function escapeHtml(valor) {
        return String(valor === null || valor === undefined ? '' : valor)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    const STATUS = {
        PENDENTE: {
            rotulo: 'Pendente',
            classes: 'bg-amber-100 text-amber-800 ring-amber-200'
        },
        ATRIBUIDO: {
            rotulo: 'Atribuido',
            classes: 'bg-sky-100 text-sky-800 ring-sky-200'
        },
        EM_TRANSITO: {
            rotulo: 'Em transito',
            classes: 'bg-indigo-100 text-indigo-800 ring-indigo-200'
        },
        ENTREGUE: {
            rotulo: 'Entregue',
            classes: 'bg-emerald-100 text-emerald-800 ring-emerald-200'
        },
        CANCELADO: {
            rotulo: 'Cancelado',
            classes: 'bg-rose-100 text-rose-800 ring-rose-200'
        }
    };

    function statusBadge(status) {
        const info = STATUS[status] || { rotulo: formatarEnum(status), classes: 'bg-slate-100 text-slate-700 ring-slate-200' };
        return '<span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset '
            + info.classes + '">' + escapeHtml(info.rotulo) + '</span>';
    }

    function vasilhameBadge(vasilhame) {
        if (vasilhame.vencido) {
            return '<span class="inline-flex items-center rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700">Vencido</span>';
        }
        const cores = {
            NA_CENTRAL: 'bg-slate-100 text-slate-700',
            EM_TRANSITO: 'bg-sky-100 text-sky-700',
            COM_CLIENTE: 'bg-emerald-100 text-emerald-700',
            DESCARTADO: 'bg-rose-100 text-rose-700'
        };
        const classe = cores[vasilhame.status] || 'bg-slate-100 text-slate-700';
        return '<span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ' + classe + '">'
            + escapeHtml(formatarEnum(vasilhame.status)) + '</span>';
    }

    function enderecoTexto(endereco) {
        if (!endereco) return 'Endereco nao cadastrado';
        const complemento = endereco.complemento ? ' - ' + endereco.complemento : '';
        return endereco.logradouro + ', ' + endereco.numero + complemento
            + ' — ' + endereco.bairro + ', ' + endereco.cidade + '/' + endereco.uf
            + ' • ' + endereco.cep + ' • Zona ' + formatarEnum(endereco.zona);
    }

    function toast(mensagem, tipo) {
        let raiz = document.getElementById('toast-root');
        if (!raiz) {
            raiz = document.createElement('div');
            raiz.id = 'toast-root';
            raiz.className = 'fixed bottom-5 right-5 z-[100] flex w-[min(92vw,380px)] flex-col gap-2';
            document.body.appendChild(raiz);
        }

        const sucesso = tipo !== 'erro';
        const div = document.createElement('div');
        div.className = 'flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg '
            + (sucesso
                ? 'border-emerald-200 bg-emerald-50 text-emerald-900'
                : 'border-rose-200 bg-rose-50 text-rose-900');
        div.innerHTML = '<span class="mt-0.5 text-lg">' + (sucesso ? '✓' : '!') + '</span>'
            + '<div class="leading-snug">' + escapeHtml(mensagem) + '</div>';
        raiz.appendChild(div);

        setTimeout(() => {
            div.style.transition = 'opacity .25s, transform .25s';
            div.style.opacity = '0';
            div.style.transform = 'translateY(6px)';
            setTimeout(() => div.remove(), 300);
        }, 4200);
    }

    function erro(mensagem) {
        toast(mensagem, 'erro');
    }

    function logout() {
        Auth.clear();
        location.href = '/login.html';
    }

    async function comBotao(button, acao) {
        if (!button) {
            return acao();
        }
        const rotulo = button.innerHTML;
        button.disabled = true;
        button.classList.add('opacity-60', 'cursor-wait');
        try {
            return await acao();
        } finally {
            button.disabled = false;
            button.classList.remove('opacity-60', 'cursor-wait');
            button.innerHTML = rotulo;
        }
    }

    window.Aqua = {
        Auth,
        API,
        homeFor,
        requireRole,
        logout,
        comBotao,
        UI: {
            money,
            formatarData,
            formatarDataHora,
            formatarEnum,
            escapeHtml,
            statusBadge,
            vasilhameBadge,
            enderecoTexto,
            toast,
            erro
        }
    };
})();
