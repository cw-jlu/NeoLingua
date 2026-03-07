1、curl -L https://micro.mamba.pm/install.sh | bash
source ~/.bashrc    
#安装 micromamba

2、micromamba create -n mfa -c conda-forge python=3.10 montreal-forced-aligner -y
micromamba activate mfa
#创建mfa环境

3、#手动上传english_us_arpa.dict、english_us_arpa.zip

4、mkdir -p ~/models
cp english_us_arpa.dict ~/models/
cp english_us_arpa.zip  ~/models/

5、cd ~/models
unzip english_us_arpa.zip    #解压文件



