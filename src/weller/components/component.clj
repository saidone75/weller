(ns weller.components.component)

(defprotocol Component
  (start [this])
  (stop [this]))
