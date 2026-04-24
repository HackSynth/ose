export function DecorativeBackground() {
  return (
    <div aria-hidden="true" className="absolute inset-0 overflow-hidden">
      <div className="ose-orb -left-24 top-20 h-72 w-72 bg-softYellow" />
      <div className="ose-orb -right-20 top-10 h-80 w-80 bg-softBlue" />
      <div className="ose-orb bottom-10 left-1/3 h-64 w-64 bg-softRose" />
      <div className="ose-orb bottom-24 right-1/4 h-48 w-48 bg-softGreen" />
    </div>
  );
}
