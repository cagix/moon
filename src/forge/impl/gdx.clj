(ns forge.impl.gdx
  (:require [forge.base :refer :all])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math MathUtils)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn-impl ttfont [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str-replace (str-upper-case (name k)) "-" "_")))))

(def ^:private k->input-button (partial static-field "Input$Buttons"))
(def ^:private k->input-key    (partial static-field "Input$Keys"))

(defn-impl equal? [a b]
  (MathUtils/isEqual a b))

(defn-impl clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn-impl degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn-impl exit-app []
  (.exit Gdx/app))

(defn-impl frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn-impl delta-time []
  (.getDeltaTime Gdx/graphics))

(defn-impl button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn-impl key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn-impl key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(defn-impl set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn-impl internal-file [path]
  (.internal Gdx/files path))
