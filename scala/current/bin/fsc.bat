@echo off

rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################

if "%OS%" NEQ "Windows_NT" (
  echo "Sorry, your version of Windows is too old to run Scala."
  goto :eof
)

@setlocal
call :set_home

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=%JAVA_HOME%\bin\java.exe
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=%JAVA_OPTS%
if "%_JAVA_OPTS%"=="" set _JAVA_OPTS=-Xmx256M -Xms32M

set _TOOL_CLASSPATH=
if "%_TOOL_CLASSPATH%"=="" (
  for %%f in ("%_SCALA_HOME%\lib\*") do call :add_cpath "%%f"
  for /d %%f in ("%_SCALA_HOME%\lib\*") do call :add_cpath "%%f"
)

set _PROPS=-Dscala.home="%_SCALA_HOME%" -Denv.emacs="%EMACS%" 

rem echo "%_JAVACMD%" %_JAVA_OPTS% %_PROPS% -cp "%_TOOL_CLASSPATH%" scala.tools.nsc.CompileClient  %*
"%_JAVACMD%" %_JAVA_OPTS% %_PROPS% -cp "%_TOOL_CLASSPATH%" scala.tools.nsc.CompileClient  %*
goto end

rem ##########################################################################
rem # subroutines

:add_cpath
  if "%_TOOL_CLASSPATH%"=="" (
    set _TOOL_CLASSPATH=%~1
  ) else (
    set _TOOL_CLASSPATH=%_TOOL_CLASSPATH%;%~1
  )
goto :eof

rem Variable "%~dps0" works on WinXP SP2 or newer
rem (see http://support.microsoft.com/?kbid=833431)
rem set _SCALA_HOME=%~dps0..
:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _SCALA_HOME=%_BIN_DIR%..
goto :eof

:end
@endlocal
