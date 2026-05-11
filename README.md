# Симуляция дорожного движения

Команды для запуска:

```
# Билд проекта
javac -d out src/*.java

# Запуск симуляции (default: Array2DInt + TCAv3, 100 steps, scale=4)
java -cp out Main

# Настраиваемый вариант, grid=[double, int], ver=[tca, v2, v3, v4]
java −cp out Main <width=144> <height=288> <steps=100> <seed=0> <scale=4> <grid=int> <ver=v3>

# Запуск визуализатора, можно управлять стрелками с клавиатуры и ставить на паузу на пробел
java -cp out Viewer
```

![Альтернативный текст](results/simulation.gif)