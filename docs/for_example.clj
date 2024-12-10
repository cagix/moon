(ns clojure.gdx.input
  (:import (com.badlogic.gdx Input)))

(defn set-processor [input processor]
  (.setInputProcessor input processor))

(defn button-just-pressed? [input b]
  (.isButtonJustPressed input b))

(defn key-just-pressed? [input k]
  (.isKeyJustPressed input k))

(defn key-pressed? [input k]
  (.isKeyPressed input k))

(defn x [input]
  (.getX input))

(defn y [input]
  (.getY input))

; =>>> private doesn't belong
; =>>> even the convinience of passing a keyword doesn't belong
; =>>> we want only a pure clojure API to the primivites
; =>>> and then forge.input uses keyword
; =>>> forge is a game engine & editor for realtime desktop 2d grid based (with optional pause) top down
; Features:
; * AI
; * Inventory
; * Skills
; * Schema (?)
; * Effects/Modifiers
