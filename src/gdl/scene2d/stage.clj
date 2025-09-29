(ns gdl.scene2d.stage
  (:import (gdl.scene2d Stage)))

(defn create [viewport batch]
  (Stage. viewport batch))

(defn set-ctx! [^Stage stage ctx]
  (set! (.ctx stage) ctx))

(defn get-ctx [^Stage stage]
  (.ctx stage))

(defn act! [^Stage stage]
  (.act stage))

(defn draw! [^Stage stage]
  (.draw stage))

(defn add! [^Stage stage actor]
  (.addActor stage actor))

(defn clear! [^Stage stage]
  (.clear stage))

(defn root [^Stage stage]
  (.getRoot stage))

(defn hit [^Stage stage [x y]]
  (.hit stage x y true))

(defn viewport [^Stage stage]
  (.getViewport stage))
