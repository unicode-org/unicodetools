#include <cstdio>
#include <cstdlib>


typedef unsigned short UShort16;

typedef unsigned int   UInt32;

extern "C" int unisift_IsAlphabetic(UInt32 c) { return 1; }
extern "C" int unisift_IsNonSpacing(UInt32 c) { return 1; }
extern "C" int unisift_IsCombining(UInt32 c) { return 1; }
extern "C" int unisift_IsExtender(UInt32 c) { return 1; }
extern "C" int unisift_IsCurrency(UInt32 c) { return 1; }
extern "C" int unisift_IsPunctuation(UInt32 c) { return 1; }
extern "C" int unisift_IsDiacritic(UInt32 c) { return 1; }
extern "C" int unisift_IsNumeric(UInt32 c) { return 1; }
extern "C" int unisift_IsDecimalDigit(UInt32 c) { return 1; }
extern "C" int unisift_IsWhiteSpace(UInt32 c) { return 1; }
extern "C" int unisift_IsMath(UInt32 c) { return 1; }
extern "C" int unisift_IsIdeographic(UInt32 c) { return 1; }
extern "C" int unisift_IsMiscSymbolic(UInt32 c) { return 1; }
extern "C" int unisift_IsIgnorable(UInt32 c) { return 1; }

extern "C" int unisift_IsUpper(UInt32 c) { return 1; }

extern "C" UInt32 unisift_ToLower(UInt32 c) { return 1; }

extern "C" UInt32 unisift_ToUpper(UInt32 c) { return 1; }

extern "C" int unisift_ToIntValue(UInt32 c) { return 1; }

extern "C" int unisift_GetCombiningClass(UInt32 c) { return 1; }