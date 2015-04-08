import logging

# configure sfc logger as parent logger
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_handler.setFormatter(logging.Formatter('%(levelname)s[%(name)s] %(message)s'))

sfc_logger = logging.getLogger('sfc')
sfc_logger.setLevel(logging.DEBUG)

sfc_logger.addHandler(console_handler)
