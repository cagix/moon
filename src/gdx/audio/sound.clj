(ns gdx.audio.sound
  (:import (com.badlogic.gdx.audio Sound)))

(def play! Sound/.play)

(comment
 (import 'com.badlogic.gdx.backends.lwjgl3.audio.OpenALSound)

 (seq (.getDeclaredFields OpenALSound))
 (require 'clojure.reflect)
 (clojure.reflect/type-reflect OpenALSound)
 )
