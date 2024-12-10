(ns anvil.screen)

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (dispose [_])
  (render  [_]))

(declare screens
         current-k)

(defn current []
  (and (bound? #'current-k)
       (current-k screens)))

(defn change
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (def current-k new-k)
    (enter screen)))

(defn setup [screens first-k]
  (def screens screens)
  (change first-k))

(defn dispose-all []
  (run! dispose (vals screens)))

(defn render-current []
  (render (current)))
