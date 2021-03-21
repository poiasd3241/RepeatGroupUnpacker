package com.poiasd;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.println("Enter the text to unpack and press ENTER:");
            String packed = input.nextLine();

            Unpacker.ValidationResult validationResult = Unpacker.isValidForUnpacking(packed);
            if (validationResult.isValid()) {
                System.out.println("Unpacked: " + Unpacker.unpack(packed));
            } else {
                System.out.println("The provided text is not valid for unpacking. Details:\n" + validationResult.message());
            }
            System.out.println();
        }
    }
}
