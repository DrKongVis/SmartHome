/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2020 STMicroelectronics.
  * All rights reserved.</center></h2>
  *
  * This software component is licensed by ST under BSD 3-Clause license,
  * the "License"; You may not use this file except in compliance with the
  * License. You may obtain a copy of the License at:
  *                        opensource.org/licenses/BSD-3-Clause
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "adc.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "string.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

//Ȼ���ڰ�����ʱ�������Ҫ4096������������������8������ǣ�����������Ҫ����4096/8=512�����У�תһȦ��
#define stepsperrev 4096 



/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

//-----------------------------ֱ�ӹ�����----------------------------------
void MotorRight(uint16_t num);				//���ת��num->Ȧ��
void MotorLeft(uint16_t num);					
void CheckState(void);

//-----------------------------��ӹ�����----------------------------------
void delay(uint16_t us);
void steper_ser_rpm(int rpm);
void steper_half_drive(int step);

void clean(void);


/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/

/* USER CODE BEGIN PV */

//----------------------------------������Ʊ���----------------------------------
//						ct_fan�����ȿ���
//						ct_light���ƹ����
//						ct_curtain����������
//						ֵ��0-->���� 1-->�ر�
//----------------------------------������Ʊ���----------------------------------
uint16_t ct_fan = 1;
uint16_t ct_light = 0;
uint16_t ct_curtain = 0;
//----------------------------------���ڱȽ�ָ���----------------------------------
uint16_t DriverMode_Fan = 0;											//��������ģʽ��0->�Զ�  1->�ֶ�
uint16_t DriverMode_Light = 0;										//�ƹ�����ģʽ��0->�Զ�  1->�ֶ�
const char ct_str_openFan[15] = "Fan_On#";
const char ct_str_closeFan[15] = "Fan_Off#";
const char ct_str_openLight[15] = "Light_On#";
const char ct_str_closeLight[15] = "Light_Off#";
const char ct_str_openCurtain[15] = "Door_On#";
const char ct_str_closeCurtain[15] = "Door_Off#";
const char ct_str_Auto_Fan[15] = "fan_auto#";
const char ct_str_NotAuto_Fan[15] = "fan_auto_no#";
const char ct_str_Auto_Light[15] = "light_auto#";
const char ct_str_NotAuto_Light[15] = "light_auto_no#";
const char getState[15] = "getState#";
//----------------------------------���ڱȽ�ָ���----------------------------------

//--------------------------------���ڱ�������--------------------------------

//����1
uint8_t uart1RxState = 1;
uint8_t uart1RxCounter = 0;
uint8_t TempBuff[1];
char uart1RxBuff[128];
char uart1Real[128];
char SendBack[128];
uint8_t hello[] = "hello Uart\r\n";

//����2
uint8_t uart2RxState = 1;
uint8_t uart2RxCounter = 0;
uint8_t TempBuff2[1];
char uart2RxBuff[128];
char SendBack2[128];
//--------------------------------���ڱ�������--------------------------------

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */
  

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_ADC1_Init();
  MX_ADC2_Init();
  MX_USART1_UART_Init();
  MX_TIM1_Init();
  MX_USART3_UART_Init();
  /* USER CODE BEGIN 2 */

	//������ʱ�����������������
	HAL_TIM_Base_Start(&htim1);

	//������ʱ���жϽ���ָ��
	HAL_UART_Receive_IT(&huart1, TempBuff, 1);
	HAL_UART_Receive_IT(&huart3, TempBuff2, 1);
	HAL_UART_Transmit(&huart3,(uint8_t *)"Hello", sizeof("Hello"), 0xFFFF);
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
		HAL_GPIO_TogglePin(debug_GPIO_Port,debug_Pin);
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
		
		//���մ���ָ��
		
		//��������
		if(ct_curtain == 1)
		{
			//ֱ�Ӳ���
			MotorLeft(3);
			ct_curtain = 0;
		}
		else if(ct_curtain == 2)
		{
			//ֱ�Ӳ���
			MotorRight(3);
			ct_curtain = 0;
		}
		
		
		if(DriverMode_Light == 0)
		{
			//----------------�Զ�ģʽʹ�ô�������ֵ���ж�-------------------
			uint16_t adc_value_light = 0;				//ADCת��ֵ
			float voltage_light = 0.0;					//��ѹֵ
			
			HAL_ADC_Start(&hadc1);
			//�ȴ�����ת����ת����ɣ��������ʱʱ�䣩
			HAL_ADC_PollForConversion(&hadc1,100);
			adc_value_light = HAL_ADC_GetValue(&hadc1);
			voltage_light = (float)adc_value_light / 4096 * 3.3 * 10;
			if(voltage_light > 7)
			{
				HAL_GPIO_WritePin(ct_light_GPIO_Port,ct_light_Pin,GPIO_PIN_SET);
			}else
			{
				HAL_GPIO_WritePin(ct_light_GPIO_Port,ct_light_Pin,GPIO_PIN_RESET);
			}
			HAL_Delay(1000);
		}
		else
		{
			if(ct_light == 1)
			{
				HAL_GPIO_WritePin(ct_light_GPIO_Port,ct_light_Pin,GPIO_PIN_SET);
			}else
			{
				HAL_GPIO_WritePin(ct_light_GPIO_Port,ct_light_Pin,GPIO_PIN_RESET);
			}
		}
		
		if(DriverMode_Fan == 0)
		{
			uint16_t adc_value_air = 0;				//ADCת��ֵ
			float voltage_air = 0.0;					//��ѹֵ
			
			HAL_ADC_Start(&hadc2);
			//�ȴ�����ת����ת����ɣ��������ʱʱ�䣩
			HAL_ADC_PollForConversion(&hadc2,100);
			adc_value_air = HAL_ADC_GetValue(&hadc2);
			voltage_air = (float)adc_value_air / 4096 * 3.3 * 10;
			if(voltage_air > 12)
			{
				HAL_GPIO_WritePin(ct_fan_GPIO_Port,ct_fan_Pin,GPIO_PIN_RESET);
			}else
			{
				HAL_GPIO_WritePin(ct_fan_GPIO_Port,ct_fan_Pin,GPIO_PIN_SET);
			}
			
			HAL_Delay(1000);
		}else
		{
			if(ct_fan == 1)
			{
				HAL_GPIO_WritePin(ct_fan_GPIO_Port,ct_fan_Pin,GPIO_PIN_RESET);
			}else
			{
				HAL_GPIO_WritePin(ct_fan_GPIO_Port,ct_fan_Pin,GPIO_PIN_SET);
			}
		}
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }
  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_ADC;
  PeriphClkInit.AdcClockSelection = RCC_ADCPCLK2_DIV6;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/* USER CODE BEGIN 4 */

//���ظ��ƶ˵�ǰ״̬
void CheckState()
{
	char state[128];
	sprintf(state,"{\"fan\":%d,\"light\":%d,\"curtain\":%d,\"Mode_Fan\":%d,\"Mode_Light\":%d}",
	ct_fan,ct_light,ct_curtain,DriverMode_Fan,DriverMode_Light);
	HAL_UART_Transmit(&huart1,(uint8_t *)&state, sizeof("{\"fan\":%d,\"light\":%d,\"curtain\":%d,\"Mode_Fan\":%d,\"Mode_Light\":%d}}")-5, 0xFFFF);
}


//�����жϻص�����
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
//void HAL_UART_RxHalfCpltCallback(UART_HandleTypeDef *huart)
{
//	HAL_GPIO_TogglePin(LED1_GPIO_Port,LED1_Pin);
	if(huart->Instance == USART1)
	{
		//����gcc/g++����
		UNUSED(huart);
		//�������
		if(uart1RxCounter >= 128)
		{
			uart1RxCounter = 0;
			memset(uart1RxBuff,0x00,sizeof(uart1RxBuff));
			HAL_UART_Transmit(&huart1,(uint8_t *)"�������",8,0xff);
			HAL_UART_Receive_IT(&huart1, TempBuff, 1);
		}
		else
		{
			uart1RxBuff[uart1RxCounter++] = TempBuff[0];
			//HAL_UART_Transmit(&huart1,TempBuff[0],1,0xff);
			if(uart1RxBuff[uart1RxCounter-1] == '#')
			{
				//HAL_UART_Transmit(&huart1,(uint8_t *)uart1RxBuff,sizeof(uart1RxBuff),0xff);
				uart1RxCounter = 0;
				uart1RxState = 1;
				///////////////////////////////////////////////////
				if(strstr((char *)uart1RxBuff,ct_str_Auto_Light) != NULL)
				{
					DriverMode_Light = 0;
				}else if(strstr((char *)uart1RxBuff,ct_str_NotAuto_Light) != NULL)
				{
					DriverMode_Light = 1;
				}else if(strstr((char *)uart1RxBuff,ct_str_Auto_Fan) != NULL)
				{
					DriverMode_Fan = 0;
				}else if(strstr((char *)uart1RxBuff,ct_str_NotAuto_Fan) != NULL)
				{
					DriverMode_Fan = 1;
				}else if(strstr((char *)uart1RxBuff,ct_str_openFan) != NULL)
				{
					ct_fan = 0;
				}else if(strstr((char *)uart1RxBuff,ct_str_closeFan) != NULL)
				{
					ct_fan = 1;
				}else if(strstr((char *)uart1RxBuff,ct_str_openLight) != NULL)
				{
					ct_light = 1;
				}else if(strstr((char *)uart1RxBuff,ct_str_closeLight) != NULL)
				{
					ct_light = 0;
				}else if(strstr((char *)uart1RxBuff,ct_str_openCurtain) != NULL)
				{
					ct_curtain = 1;
				}else if(strstr((char *)uart1RxBuff,ct_str_closeCurtain) != NULL)
				{
					ct_curtain = 2;
				}else if(strstr((char *)uart1RxBuff,"connected#") != NULL)
				{
					HAL_UART_Transmit(&huart1,(uint8_t *)"connected", sizeof("connected"), 0xFFFF);
				}
				//HAL_Delay(100);
				CheckState();
				//sprintf(SendBack,"ָ�%s \r\n",(char *)uart1RxBuff);
				//HAL_UART_Transmit(&huart1,(uint8_t *)SendBack,sizeof(SendBack),0xff);
				//while(HAL_UART_GetState(&huart1) == HAL_UART_STATE_BUSY_TX);
				
				//memset(SendBack,0x00,sizeof(SendBack));
				//strcpy(uart1Real,uart1RxBuff);
				memset(uart1RxBuff,0x00,sizeof(uart1RxBuff)+1);
			}
		}
		memset(TempBuff,0x00,sizeof(TempBuff));
		HAL_UART_Receive_IT(&huart1, TempBuff, 1);
	}
	
	if(huart->Instance == USART3)
	{
		//HAL_UART_Transmit(&huart2,(uint8_t *)"2b\r\n",sizeof("2b\r\n"),0xff);
		//---------------------------------------��������------------------------
		//����gcc/g++����
		UNUSED(huart);
		//�������
		if(uart2RxCounter >= 128)
		{
			uart2RxCounter = 0;
			memset(uart2RxBuff,0x00,sizeof(uart2RxBuff));
			HAL_UART_Transmit(&huart3,(uint8_t *)"�������",8,0xff);
			HAL_UART_Receive_IT(&huart3, TempBuff2, 1);
		}
		else
		{
			uart2RxBuff[uart2RxCounter++] = TempBuff2[0];
			//HAL_UART_Transmit(&huart3,(uint8_t *)"132\r\n",3,0xff);
			if(uart2RxBuff[uart2RxCounter-1] == '#')
			{
				HAL_UART_Transmit(&huart3,(uint8_t *)uart2RxBuff,sizeof(uart2RxBuff),0xff);
				memset(uart2RxBuff,0x00,sizeof(uart2RxBuff));
				uart2RxCounter = 0;
				
				if(strcmp((char *)uart2RxBuff,ct_str_openFan) == 0)
				{
					ct_fan = 1;
				}else if(strcmp((char *)uart2RxBuff,ct_str_closeFan) == 0)
				{
					ct_fan = 0;
				}else if(strcmp((char *)uart2RxBuff,ct_str_openLight) == 0)
				{
					ct_light = 1;
				}else if(strcmp((char *)uart2RxBuff,ct_str_closeLight) == 0)
				{
					ct_light = 0;
				}else if(strcmp((char *)uart2RxBuff,ct_str_openCurtain) == 0)
				{
					ct_curtain = 1;
				}else if(strcmp((char *)uart2RxBuff,ct_str_closeCurtain) == 0)
				{
					ct_curtain = 2;
				}
					
				CheckState();
				//sprintf(SendBack2,"ָ�%s \r\n",(char *)uart2RxBuff);
				//HAL_UART_Transmit(&huart3,(uint8_t *)SendBack,sizeof(SendBack),0xff);
				while(HAL_UART_GetState(&huart3) == HAL_UART_STATE_BUSY_TX);
				
				//memset(SendBack2,0x00,sizeof(SendBack2));
				
			}
			HAL_UART_Receive_IT(&huart3, TempBuff2, 1);
		}
		memset(TempBuff2,0x00,sizeof(TempBuff2));
	}
}

void MotorLeft(uint16_t num)
{
	for(int c=0; c<num; c++)
	{
		for(int i=0; i<512; i++)		//ת360��
		{
			for(int j=0; j<8; j++)
			{
				steper_half_drive(j);
				steper_ser_rpm(13);
			}
		}
	}
	clean();
}

void MotorRight(uint16_t num)
{
	for(int c=0; c<num; c++)
	{
		for(int i=0; i<512; i++)		//ת360��
			{
				for(int j=7; j>=0; j--)
				{
					steper_half_drive(j);
					steper_ser_rpm(13);
				}
			}
	}
	clean();
}

void clean(void)
{
	HAL_GPIO_WritePin(GPIOB,A_Pin,GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOB,B_Pin,GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOB,C_Pin,GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOB,D_Pin,GPIO_PIN_RESET);
}

void delay(uint16_t us)
{
	__HAL_TIM_SET_COUNTER(&htim1,0);		//��ʱ������
	while(__HAL_TIM_GET_COUNTER(&htim1)<us);
}

//�ٶȵ���
void steper_ser_rpm(int rpm)//rpm-->ÿ���ӵ�Ȧ����max==13��
{
	delay(60000000/stepsperrev/rpm);
}

void steper_half_drive(int step)
{
	switch(step)
	{
		case 0:
			HAL_GPIO_WritePin(GPIOB,A_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,B_Pin|C_Pin|D_Pin,GPIO_PIN_RESET);
			break;
		case 1:
			HAL_GPIO_WritePin(GPIOB,A_Pin|B_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,C_Pin|D_Pin,GPIO_PIN_RESET);
			break;
		case 2:
			HAL_GPIO_WritePin(GPIOB,B_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,D_Pin|C_Pin|A_Pin,GPIO_PIN_RESET);
			break;
		case 3:
			HAL_GPIO_WritePin(GPIOB,B_Pin|C_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,A_Pin|D_Pin,GPIO_PIN_RESET);
			break;
		case 4:
			HAL_GPIO_WritePin(GPIOB,C_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,B_Pin|A_Pin|D_Pin,GPIO_PIN_RESET);
			break;
		case 5:
			HAL_GPIO_WritePin(GPIOB,D_Pin|C_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,B_Pin|A_Pin,GPIO_PIN_RESET);
			break;
		case 6:
			HAL_GPIO_WritePin(GPIOB,D_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,B_Pin|C_Pin|A_Pin,GPIO_PIN_RESET);
			break;
		case 7:
			HAL_GPIO_WritePin(GPIOB,A_Pin|D_Pin,GPIO_PIN_SET);
			HAL_GPIO_WritePin(GPIOB,B_Pin|C_Pin,GPIO_PIN_RESET);
			break;
	}
}


/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */

  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{ 
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     tex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
