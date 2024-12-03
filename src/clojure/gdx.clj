(ns clojure.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.assets AssetManager)))

(defn static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(def ^:private k->input-button (partial static-field "Input$Buttons"))
(def ^:private k->input-key    (partial static-field "Input$Keys"))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn exit-app []
  (.exit Gdx/app))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn input-x []
  (.getX Gdx/input))

(defn input-y []
  (.getY Gdx/input))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.ILookup] []
    (valAt [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(def play Sound/.play)
