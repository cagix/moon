(ns forge.screen)

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (destroy [_]))
