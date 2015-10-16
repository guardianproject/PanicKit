
PanicKit is a collection of tools for letting panic trigger and panic receiver
apps safely and easily connect to each other. The trigger apps are the part
that the user will actual engage when in a panic situation. The receiver apps
receive the trigger signal from the trigger apps when the user has initiated
the panic response. The connections between trigger and receiver can be
strictly enforced based on `packageName` and APK signing key.

https://dev.guardianproject.info/projects/panic/wiki


License
-------

This library is licensed under the LGPLv2.1.  We believe this is compatible
with all reasonable uses, including proprietary software, but let us know if
it provides difficulties for you.  For more info on how that works with Java,
see:

https://www.gnu.org/licenses/lgpl-java.en.html
