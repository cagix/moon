(ns cdq.ui.stage
  (:import (cdq.ui Stage)))

(defn create [viewport batch]
  (Stage. viewport batch))

(defn ctx [^Stage stage]
  (.ctx stage))

(defn set-ctx! [^Stage stage ctx]
  (set! (.ctx stage) ctx))

(defn act! [^Stage stage]
  (.act stage))

(defn draw! [^Stage stage]
  (.draw stage))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))
