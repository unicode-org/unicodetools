#include <unicode/ustdio.h>
#include <stdio.h>
#include <unicode/uniset.h>
#include <unicode/uchar.h>

using namespace icu;

int main(int argc, const char* argv[]) {
  UErrorCode status = U_ZERO_ERROR;
  UnicodeSet validSet(UnicodeString("[^[:c:][:z:][:di:]]",""), status);
  if(U_FAILURE(status)) {
    printf(";UnicodeSet failed: %s\n", u_errorName(status));
    return 4;
  }
	// fprintf(stderr, "=size=%d\n", validSet.size());
  if(argc != 2) {
    puts(";Error: need 1 argument");
    return 1;
  } else {
    int cp = -1;
    if(sscanf(argv[1],"%x", &cp) != 1) {
      puts(";Error: bad codepoint");
      return 2;
    }
    UChar32 c = cp;
	// fprintf(stderr, "U+%04X\n", c);
    if(!validSet.contains(c)) {
      printf(";Not registerable: U+%04X\n", c);
      return 8;
    }
    char buf[200];
    int n= u_charName(c, U_UNICODE_CHAR_NAME, buf, 200, &status);
    if(U_FAILURE(status)) {
      printf("%04X;VALID\n", c);
      return 0;
    } else {
      printf("%04X;%s\n", c, buf);
      return 0;
    }
  }
}
