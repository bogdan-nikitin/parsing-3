void foo() {
    return;
}

int bar() {
    return 42;
}

int main() {
    int a, b, c = 1, d, e = 11 * 4;

    b = c;

    if (a == b) {
        d = 1;
    }
    if (c != d) ++a;

    if (a == 1) {
        foo();
    } else {
        b--;
    }

    if (a == 1) {
        a;
    } else if (a == 2) {
        b;
    } else {
        c;
    }

    for (int i = 0; i < d; ++i) {
        foo();
        foo();
    }

    for (int i = 0; i > e + d; i++) foo();

    while (1 != 1) {
        foo();
    }

    while (42) foo();

    if (a == 1) {
        return 0;
    }

    {
        int x = bar();
    }
}

