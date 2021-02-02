# Phaser
A library for use in macro code to facilitate clean and comfy value-oriented metaprogramming - that is, computing as much of the output as possible at macro expansion time,
as in Dottytags, which was where this idea had its inception. I have further plans for more metaprogramming framework stuff, like passing arbitrary state between
nested macro calls using erased parameters, and perhaps working out an optional Free (or perhaps even regular) Monad for `Phaser`/`Phunction` to enable monadic
for-comprehension syntax. I will also be backporting this to Dottytags to make it much more elegant and to serve as a proof of concept of sorts.