from setuptools import setup, find_namespace_packages

description = "Speechbrain inference module"
install_requires = [
    "pyignite==0.6.0",
    "torch==1.13.0",
    "torchaudio==0.13.0",
    "speechbrain",
    "aiokafka",
    "confluent-kafka"
    "typing"
]
test_require = [
    "testcontainers",
    "pytest"
]
extras = {
    'test': test_require
}
setup(
    name="spbrain",
    version="0.0.2",
    packages=find_namespace_packages(),
    description=description,
    long_description=description,
    long_description_content_type="text/x-rst",
    url="https://github.com/limdongjin/ignkafasr",
    install_requires=install_requires,
    python_requires=">=3.7",
    extras=extras
)
