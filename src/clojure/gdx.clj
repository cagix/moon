(ns clojure.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.utils Disposable)))

(defn exit-app []
  (.exit Gdx/app))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(def dispose Disposable/.dispose)

(defn gdx-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(def ^:private gdx-input-button (partial gdx-field "Input$Buttons"))
(def ^:private gdx-input-key    (partial gdx-field "Input$Keys"))

(defn button-just-pressed?
  ":left, :right, :middle, :back or :forward."
  [b]
  (.isButtonJustPressed Gdx/input (gdx-input-button b)))

(defn key-just-pressed?
  "See [[key-pressed?]]."
  [k]
  (.isKeyJustPressed Gdx/input (gdx-input-key k)))

(defn key-pressed?
  "For options see [libgdx Input$Keys docs](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/Input.Keys.html).
  Keys are upper-cased and dashes replaced by underscores.
  For example Input$Keys/ALT_LEFT can be used with :alt-left.
  Numbers via :num-3, etc."
  [k]
  (.isKeyPressed Gdx/input (gdx-input-key k)))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))
