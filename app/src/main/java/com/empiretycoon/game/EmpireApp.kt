package com.empiretycoon.game

import android.app.Application

/**
 * Application root. En esta fase no se inicializa nada costoso —
 * el ViewModel se encarga de cargar el save cuando se crea.
 */
class EmpireApp : Application()
