int& bar(int& x) {
    return x;
}


int main() {
    int a = 1;
    int b = 2;
    int c = 3;
    int d;
    d = a + b;
    d = d - b;
    d = a * b;
    d = a / b;
    d = a + b * d - c;
    d *= 42 + -42;
    d /= (((42)));
    d -= ++d;
    d += /* comment */ (a + b) * d;
    int *x;
    *x += 11;
    bar(a)++;
    bar(a) += bar(a) * 4;
    int z = a == b;
    z = a < b;
}