import 'package:flutter/material.dart';
void main () {
   runApp (const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp ({super.key});
  @override 
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: CalculatorPage(),
    );
  }
}

class CalculatorPage extends StatefulWidget {
  const CalculatorPage({super.key});
  @override
  State<CalculatorPage> createState() => _CalculatorPageState();

}

class _CalculatorPageState extends State<CalculatorPage> {
  final TextEditingController number1 =TextEditingController(text: '2');
  final TextEditingController number2 =TextEditingController(text: '2');
String? selectedOperator;
String results = ''; 
void calculate(String operator) {
  double? a = double.tryParse(number1.text.replaceAll(',', '.'));
  double? b = double.tryParse(number2.text.replaceAll(',', '.'));

  setState((){
    selectedOperator = operator;
    if ( a == null || b == null){
      results = " vui long nhap du hai so";
      return;
    }
    double answer;
    if (operator == '+') {
      answer = a + b;
    } else if (operator == '-') {
      answer = a - b;

    } else if (operator == '*') {
      answer = a*b;
    } else { 
      if (b==0){
        results = "khong the chia cho 0";
        return;
      
      }
      answer = a/b;
    }
    if (answer == answer.truncateToDouble()) {
      results = answer.toInt().toString();
    } else { 
      results = answer.toStringAsFixed(6)
            .replaceFirst(RegExp(r'0+$'), '') // xoa cac so 0 o cuoi
            .replaceFirst(RegExp(r'\.$'), ''); // xoa dau cham neu o cuoi
      }
      
    });

}


Widget buildNumberField(TextEditingController controller) {
    return SizedBox(
      height: 48,
      child: TextField(
        controller: controller,
        keyboardType: const TextInputType.numberWithOptions(
          decimal: true,
          signed: true,
        ),
        decoration: InputDecoration(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 14,
            vertical: 12,
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(
              color: Colors.grey,
            ),
          ),
        ),
        onChanged: (value) {
          if (selectedOperator != null) {
            calculate(selectedOperator!);
          }
        },
      ),
    );
  }

Widget buildOperatorButton(String symbol, Color color,){
    bool isSelected = selectedOperator == symbol;

    return GestureDetector(
      onTap: () {
        calculate(symbol);
      },
      child: Container(
        width: 48,
        height: 46,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(6),
          border: Border.all(
            color: isSelected ? Colors.black : Colors.transparent,
            width: 3,
          ),
        ),
      child: Text(symbol, style: const TextStyle(color: Colors.white, fontSize: 18,fontWeight: FontWeight.bold,
       ),),),);
       }

@override
Widget build(BuildContext context){
  return Scaffold(
    backgroundColor: Colors.white,
    body: SafeArea(child: SingleChildScrollView(child:Padding(padding: const EdgeInsets.symmetric(horizontal: 20,),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox( height: MediaQuery.of(context).size.height *0.14,),

        const Center(child: Text('Thực hành 3', style: TextStyle(fontSize: 16, color: Colors.black,),),),
        const SizedBox(height: 55),

        buildNumberField(number1),
        const SizedBox(height: 20),

        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            buildOperatorButton(
              '+',
              const Color(0xFFD94C48),
            ),
            buildOperatorButton(
              '-',
              const Color(0xFF202020)
            ),
            buildOperatorButton(
              '*',
              const Color(0xFF5D41DD)
            ),
            buildOperatorButton(
              '/',
              const Color.fromARGB(255, 62, 139, 32)
            ),
          ],
        ),

        const SizedBox(height: 20),
        buildNumberField(number2),
        const SizedBox(height: 12),

      
        Text(
          'kết quả: $results',
          style: const TextStyle(
            fontSize: 15,
            color: Colors.black,),),
              ],
            ),
          ),
        ),
      ),
    );
  }
@override
void dispose(){
  number1.dispose();
  number2.dispose();
  super.dispose();
}
}