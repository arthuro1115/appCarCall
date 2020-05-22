//Programação Modulo Bluetooth HC-05
#include <SoftwareSerial.h>

//Define o pino que sera ligado os led's das setas
#define ledFrontal 5
#define ledTraseiro 7

//Definindo o a porta que o botao sera inserido
#define btColid 8

//Definindo os Pino de Envio(TX) e Recepção(RX) de dados
#define TX_PIN 3 
#define RX_PIN 2

//Variavel
int btState = 0;
int btPress = 0;

//Declarando meu modulo HC-05
SoftwareSerial meuBluetooth(RX_PIN,TX_PIN); 


void setup() {

  //Iniciando a comunicação Serial 
  Serial.begin(9600);
  meuBluetooth.begin(9600);
  //alguns bluetooth (34800)
  
  pinMode(ledFrontal,OUTPUT);
  pinMode(ledTraseiro,OUTPUT);
  pinMode(btColid,INPUT_PULLUP);

}

void loop() {

  //delay(1000);
  btState = digitalRead(btColid);
  //Serial.println(btState);
  //delay(1000);
  
  if(meuBluetooth.available()){     
  //Protocolo { }
  //InfoStart  
  meuBluetooth.println("{");
    
  if(btState == 0){
  
    meuBluetooth.println("bateu");
    btPress = 1;
    ligaSeta();
    }

   /* while(btPress == 1){
      
      delay(2000);
      meuBluetooth.println("bateu");
      Serial.println("Bateu");
      ligaSeta();
      }
      */

  //InfoEnd
  meuBluetooth.println("}");

  }
  //Buffer de informações necessario para o processamento 
  delay(2000);
} 

//Função de ligar as setas
void ligaSeta(){

    digitalWrite(ledFrontal,HIGH);
    digitalWrite(ledTraseiro,HIGH);
    delay(500);
    digitalWrite(ledFrontal,LOW);
    digitalWrite(ledTraseiro,LOW);
    delay(500);
    
}
