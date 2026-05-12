import logging
import sys

def get_logger(name: str):
    """
    Configura e retorna um logger padronizado.
    """
    logger = logging.getLogger(name)
    
    # Evita duplicar logs se a função for chamada mais de uma vez
    if not logger.hasHandlers():
        logger.setLevel(logging.INFO)
        
        # Handler para jogar os logs no console (saída padrão do Docker)
        console_handler = logging.StreamHandler(sys.stdout)
        
        # Formato: [DATA HORA] [NÍVEL] [NOME_DO_ARQUIVO] - MENSAGEM
        formatter = logging.Formatter(
            "%(asctime)s [%(levelname)s] [%(name)s] - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )
        
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)
        
    return logger