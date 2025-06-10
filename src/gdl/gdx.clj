; our platform / system abstraction
; .... 'clojure.app' ?
(ns gdl.gdx)

(defprotocol Application
  (post-runnable! [_ runnable]))

(defprotocol Files
  (internal [_ path]))

(defprotocol Audio
  (sound [_ path]))

(defprotocol Graphics
  (cursor [_ path [hotspot-x hotspot-y]]))
