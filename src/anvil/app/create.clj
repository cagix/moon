(ns anvil.app.create
  (:require [anvil.app :as app]))

(defn db       [_])
(defn assets   [_])
(defn graphics [_])
(defn ui       [_])
(defn stage    [ ])
(defn world    [_])

(defn-impl app/create [config]
  (db       (:db       config))
  (assets   (:assets   config))
  (graphics (:graphics config))
  (ui       (:ui       config))
  (stage)
  (world    (:world    config)))
