(ns forge.screen)

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (dispose [_]))
