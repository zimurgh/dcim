{
  description = "DCIM development shells";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      ng = pkgs.writeShellScriptBin "ng" ''
        exec ${pkgs.pnpm}/bin/pnpm dlx @angular/cli "$@"
      '';
    in
    {
      devShells.${system} = {
        default = pkgs.mkShell {
          packages = with pkgs; [
            jdk25
            gradle_9
            nodejs_22
            pnpm
            ng
            tlaplus18
            tlafmt
          ];
          shellHook = ''
            export JAVA_HOME="${pkgs.jdk25.home}"
            export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
          '';
        };
        tla = pkgs.mkShell {
          packages = with pkgs; [ tlaplus18 tlafmt ];
        };
      };
    };
}
