(ns cdq.ui.stage
  (:require [clojure.gdx.scene2d.stage :as stage])
  (:import (cdq.ui Stage)))

(defn create [viewport batch]
  (Stage. viewport batch))

(defn ctx [^Stage stage]
  (.ctx stage))

(defn set-ctx! [^Stage stage ctx]
  (set! (.ctx stage) ctx))

(defn act! [stage]
  (stage/act! stage))

(defn draw! [stage]
  (stage/draw! stage))

(defn add-actor! [stage actor]
  (stage/add-actor! stage actor))

(defn viewport [stage]
  (stage/viewport stage))

(defn root [stage]
  (stage/root stage))

(defn hit [stage position touchable?]
  (stage/hit stage position touchable?))

(defn clear! [stage]
  (stage/clear! stage))
