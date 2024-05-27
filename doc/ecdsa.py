import random


def find_inverse(number, modulus):
    return pow(number, -1, modulus)


class Point:
    def __init__(self, x, y, curve_config):
        a = curve_config['a']
        b = curve_config['b']
        p = curve_config['p']

        if (y ** 2) % p != (x ** 3 + a * x + b) % p:
            raise Exception("The point is not on the curve")

        self.x = x
        self.y = y
        self.curve_config = curve_config

    def is_equal_to(self, point):
        return self.x == point.x and self.y == point.y

    def add(self, point):
        p = self.curve_config['p']

        if self.is_equal_to(point):
            slope = (3 * point.x ** 2) * find_inverse(2 * point.y, p) % p
        else:
            slope = (point.y - self.y) * find_inverse(point.x - self.x, p) % p

        x = (slope ** 2 - point.x - self.x) % p
        y = (slope * (self.x - x) - self.y) % p
        return Point(x, y, self.curve_config)

    def multiply(self, times):
        current_point = self
        current_coefficient = 1

        pervious_points = []
        while current_coefficient < times:
            # store current point as a previous point
            pervious_points.append((current_coefficient, current_point))
            # if we can multiply our current point by 2, do it
            if 2 * current_coefficient <= times:
                current_point = current_point.add(current_point)
                current_coefficient = 2 * current_coefficient
            # if we can't multiply our current point by 2, let's find the biggest previous point to add to our point
            else:
                next_point = self
                next_coefficient = 1
                for (previous_coefficient, previous_point) in pervious_points:
                    if previous_coefficient + current_coefficient <= times:
                        if previous_point.x != current_point.x:
                            next_coefficient = previous_coefficient
                            next_point = previous_point
                current_point = current_point.add(next_point)
                current_coefficient = current_coefficient + next_coefficient

        return current_point


secp256k1_curve_config = {
    'a': 0,
    'b': 7,
    'p': 115792089237316195423570985008687907853269984665640564039457584007908834671663
}
x = 55066263022277343669578718895168534326250603453777594175500187360389116729240
y = 32670510020758816978083085130507043184471273380659243275938904335757337482424
n = 115792089237316195423570985008687907852837564279074904382605163141518161494337
g_point = Point(x, y, secp256k1_curve_config)


def sign_message(message, private_key, k):
#    k = random.randint(1, n)
    r_point = g_point.multiply(k)
    r = r_point.x % n
    if r == 0:
        return sign_message(message, private_key, random.randint(1, n))
    k_inverse = find_inverse(k, n)
    s = k_inverse * (message + r * private_key) % n
    return r, s


def verify_signature(signature, message, public_key):
    (r, s) = signature
    s_inverse = find_inverse(s, n)
    u = message * s_inverse % n
    v = r * s_inverse % n
    c_point = g_point.multiply(u).add(public_key.multiply(v))
    return c_point.x == r

# test starts here
private_key = 123456789012345  # any random integer
public_key = g_point.multiply(private_key)
message = 1234567890 # any integer
k = 22
signature = sign_message(message, private_key, k)

print('message: ', message)
print('private_key: ', private_key)
print('public_key: ', public_key.x)
print('k: ', k)
print('signature: ', signature)
print('is valid: ', verify_signature(signature, message, public_key))

message = random.randint(1, 115792089237316195423570985008687907852837564279074904382605163141518161494337)
print('message: ', message)

print('private_key,public_key,k,signature,valid')
i=0
while i<10:
    private_key = random.randint(1, 115792089237316195423570985008687907852837564279074904382605163141518161494337)
    public_key = g_point.multiply(private_key)
    k = random.randint(1, n)
    signature = sign_message(message, private_key, k)
    valid = verify_signature(signature, message, public_key)
    print(private_key)
    print(public_key.x)
    print(k)
    print(signature)
    print(valid)
    i=i+1
